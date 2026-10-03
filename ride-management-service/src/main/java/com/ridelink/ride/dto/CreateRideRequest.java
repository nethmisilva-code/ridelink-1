package com.ridelink.ride.dto;

import com.ridelink.ride.model.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The passenger is taken from the JWT, and the estimated fare from Fare &amp;
 * Payment Service, so neither can be supplied (or faked) by the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRideRequest {

    @Schema(example = "Nadeesha Perera")
    @Size(max = 100)
    private String riderName;

    @Schema(example = "+94771234567")
    @Size(max = 20)
    private String riderPhone;

    @NotNull(message = "pickupLocation is required")
    @Valid
    private LocationDto pickupLocation;

    @NotNull(message = "destinationLocation is required")
    @Valid
    private LocationDto destinationLocation;

    @Schema(example = "Negombo", description = "Area used to find nearby drivers")
    @NotBlank(message = "serviceArea is required")
    @Size(max = 50)
    private String serviceArea;

    @Schema(example = "SEDAN")
    @NotNull(message = "vehicleType is required (TUK, SEDAN, VAN or SUV)")
    private VehicleType vehicleType;
}
