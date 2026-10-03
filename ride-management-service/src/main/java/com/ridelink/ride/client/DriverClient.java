package com.ridelink.ride.client;

import com.ridelink.ride.client.dto.AvailableDriver;
import com.ridelink.ride.exception.DownstreamServiceException;
import com.ridelink.ride.model.VehicleType;
import com.ridelink.ride.security.ServiceTokenProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Synchronous REST client for Driver &amp; Vehicle Service, authenticated with a
 * SERVICE token. Used to find eligible drivers, reserve the chosen one
 * (ON_TRIP) and release them (AVAILABLE) when the ride ends.
 */
@Component
public class DriverClient {

    private static final String SERVICE = "Driver & Vehicle Service";

    private final RestClient restClient;
    private final ServiceTokenProvider tokens;

    public DriverClient(RestClient driverRestClient, ServiceTokenProvider tokens) {
        this.restClient = driverRestClient;
        this.tokens = tokens;
    }

    public List<AvailableDriver> findAvailableDrivers(String serviceArea, VehicleType vehicleType) {
        try {
            AvailableDriver[] drivers = restClient.get()
                    .uri(uri -> uri.path("/api/v1/drivers/available")
                            .queryParam("serviceArea", serviceArea)
                            .queryParam("vehicleType", vehicleType.name())
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, tokens.bearerHeader())
                    .retrieve()
                    .body(AvailableDriver[].class);
            return drivers == null ? List.of() : List.of(drivers);
        }
        catch (RestClientResponseException ex) {
            throw new DownstreamServiceException(HttpStatus.BAD_GATEWAY,
                    SERVICE + " rejected the driver lookup (HTTP " + ex.getStatusCode().value() + ")");
        }
        catch (RestClientException ex) {
            throw unavailable("no driver can be assigned right now");
        }
    }

    /**
     * Marks the driver ON_TRIP. Returns false when Driver &amp; Vehicle Service
     * answers 409 (the driver was taken or went offline in the meantime), so
     * the caller can try the next nearest driver.
     */
    public boolean reserve(String driverId) {
        try {
            setAvailability(driverId, "ON_TRIP");
            return true;
        }
        catch (HttpClientErrorException.Conflict ex) {
            return false;
        }
        catch (RestClientResponseException ex) {
            throw new DownstreamServiceException(HttpStatus.BAD_GATEWAY,
                    SERVICE + " refused to reserve driver " + driverId + " (HTTP " + ex.getStatusCode().value() + ")");
        }
        catch (RestClientException ex) {
            throw unavailable("driver " + driverId + " could not be reserved");
        }
    }

    /** Puts the driver back to AVAILABLE. Throws DownstreamServiceException on failure. */
    public void release(String driverId) {
        try {
            setAvailability(driverId, "AVAILABLE");
        }
        catch (RestClientException ex) {
            throw new DownstreamServiceException("Driver " + driverId + " could not be released: " + ex.getMessage());
        }
    }

    private void setAvailability(String driverId, String availability) {
        restClient.patch()
                .uri("/api/v1/drivers/{id}/availability", driverId)
                .header(HttpHeaders.AUTHORIZATION, tokens.bearerHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("availability", availability))
                .retrieve()
                .toBodilessEntity();
    }

    private static DownstreamServiceException unavailable(String consequence) {
        return new DownstreamServiceException(SERVICE + " is unavailable, so " + consequence + ". Try again shortly.");
    }
}
