package com.ridelink.fare.dto;

import java.math.BigDecimal;

import com.ridelink.fare.model.VehicleType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Sent by Ride Management Service when a ride is completed. */
public record FinalFareRequest(
        @NotBlank(message = "rideId is required") String rideId,
        @NotBlank(message = "passengerId is required") String passengerId,
        String driverId,
        @NotBlank(message = "driverAccountId is required") String driverAccountId,
        @NotNull(message = "vehicleType is required") VehicleType vehicleType,
        @NotNull(message = "distanceKm is required")
        @DecimalMin(value = "0.0", message = "distanceKm cannot be negative")
        @DecimalMax(value = "500.0", message = "distanceKm must be at most 500") BigDecimal distanceKm,
        @NotNull(message = "durationMinutes is required")
        @Min(value = 1, message = "durationMinutes must be at least 1")
        @Max(value = 1440, message = "durationMinutes must be at most 1440") Integer durationMinutes) {
}
