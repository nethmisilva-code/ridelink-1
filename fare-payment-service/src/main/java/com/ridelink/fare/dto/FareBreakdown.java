package com.ridelink.fare.dto;

import java.math.BigDecimal;

import com.ridelink.fare.model.VehicleType;

/** Result of applying the fare rule; returned by the estimate endpoint. */
public record FareBreakdown(
        VehicleType vehicleType,
        BigDecimal distanceKm,
        int estimatedDurationMinutes,
        BigDecimal baseFare,
        BigDecimal distanceCharge,
        BigDecimal timeCharge,
        BigDecimal totalFare,
        String currency,
        String rule) {
}
