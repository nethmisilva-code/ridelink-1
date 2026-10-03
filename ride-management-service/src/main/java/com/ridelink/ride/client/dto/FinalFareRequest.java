package com.ridelink.ride.client.dto;

import com.ridelink.ride.model.VehicleType;

/** Request contract of Fare &amp; Payment Service POST /api/v1/fares/final. */
public record FinalFareRequest(
        String rideId,
        String passengerId,
        String driverId,
        String driverAccountId,
        VehicleType vehicleType,
        double distanceKm,
        int durationMinutes) {
}
