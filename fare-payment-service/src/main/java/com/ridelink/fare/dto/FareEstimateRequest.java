package com.ridelink.fare.dto;

import java.math.BigDecimal;

import com.ridelink.fare.model.VehicleType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** Provide either both pickup and destination coordinates, or a distance in km. */
public record FareEstimateRequest(
        @Valid Coordinates pickup,
        @Valid Coordinates destination,
        @Schema(description = "Optional instead of coordinates", example = "8.5", nullable = true)
        @DecimalMin(value = "0.0", message = "distanceKm cannot be negative")
        @DecimalMax(value = "500.0", message = "distanceKm must be at most 500") BigDecimal distanceKm,
        @Schema(example = "SEDAN")
        @NotNull(message = "vehicleType is required (TUK, SEDAN, VAN or SUV)") VehicleType vehicleType) {
}
