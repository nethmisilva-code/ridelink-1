package com.ridelink.ride.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ridelink.ride.model.VehicleType;

import java.time.Instant;

/** Response contract of Driver &amp; Vehicle Service GET /api/v1/drivers/available. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AvailableDriver(
        String driverId,
        String accountId,
        String serviceArea,
        double latitude,
        double longitude,
        Instant locationUpdatedAt,
        String vehicleId,
        String plateNumber,
        VehicleType vehicleType,
        int seats) {
}
