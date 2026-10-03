package com.ridelink.fare.dto;

import java.math.BigDecimal;

import com.ridelink.fare.model.VehicleType;

public record RateCardEntry(VehicleType vehicleType, BigDecimal baseFare, BigDecimal perKm, BigDecimal perMinute,
        String currency) {
}
