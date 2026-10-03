package com.ridelink.ride.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteRideRequest {

    /** Simulated odometer reading. Optional: falls back to the estimated distance. */
    @Schema(example = "9.5", nullable = true)
    @DecimalMin(value = "0.1", message = "actualDistanceKm must be at least 0.1")
    @DecimalMax(value = "500.0", message = "actualDistanceKm must be at most 500")
    private Double actualDistanceKm;
}
