package com.ridelink.ride.client;

import com.ridelink.ride.client.dto.Coordinates;
import com.ridelink.ride.client.dto.FareEstimateRequest;
import com.ridelink.ride.client.dto.FareQuote;
import com.ridelink.ride.client.dto.FinalFare;
import com.ridelink.ride.client.dto.FinalFareRequest;
import com.ridelink.ride.exception.DownstreamServiceException;
import com.ridelink.ride.model.Location;
import com.ridelink.ride.model.VehicleType;
import com.ridelink.ride.security.ServiceTokenProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Synchronous REST client for Fare &amp; Payment Service. The fare is always
 * calculated there (single source of truth for pricing), never by the client.
 */
@Component
public class FareClient {

    private static final String SERVICE = "Fare & Payment Service";

    private final RestClient restClient;
    private final ServiceTokenProvider tokens;

    public FareClient(RestClient fareRestClient, ServiceTokenProvider tokens) {
        this.restClient = fareRestClient;
        this.tokens = tokens;
    }

    public FareQuote estimate(Location pickup, Location destination, VehicleType vehicleType) {
        FareEstimateRequest request = new FareEstimateRequest(
                new Coordinates(pickup.getLatitude(), pickup.getLongitude()),
                new Coordinates(destination.getLatitude(), destination.getLongitude()),
                vehicleType);
        FareQuote quote = post("/api/v1/fares/estimate", request, FareQuote.class, "the fare cannot be estimated");
        if (quote == null || quote.totalFare() == null) {
            throw new DownstreamServiceException(HttpStatus.BAD_GATEWAY, SERVICE + " returned an empty fare estimate");
        }
        return quote;
    }

    /** Idempotent on the Fare side: repeating it for the same ride returns the same pending payment. */
    public FinalFare calculateFinalFare(FinalFareRequest request) {
        FinalFare fare = post("/api/v1/fares/final", request, FinalFare.class, "the ride cannot be completed");
        if (fare == null || fare.amount() == null) {
            throw new DownstreamServiceException(HttpStatus.BAD_GATEWAY, SERVICE + " returned an empty final fare");
        }
        return fare;
    }

    private <T> T post(String path, Object body, Class<T> type, String consequence) {
        try {
            return restClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, tokens.bearerHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(type);
        }
        catch (RestClientResponseException ex) {
            throw new DownstreamServiceException(HttpStatus.BAD_GATEWAY,
                    SERVICE + " rejected the request (HTTP " + ex.getStatusCode().value() + "), so " + consequence);
        }
        catch (RestClientException ex) {
            throw new DownstreamServiceException(SERVICE + " is unavailable, so " + consequence + ". Try again shortly.");
        }
    }
}
