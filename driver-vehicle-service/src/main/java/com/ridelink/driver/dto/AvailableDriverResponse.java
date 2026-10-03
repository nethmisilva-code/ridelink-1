package com.ridelink.driver.dto;

import com.ridelink.driver.model.VehicleType;

import java.time.Instant;

/** Contract consumed by Ride Management Service when it selects a driver. */
public record AvailableDriverResponse(
        String driverId,
        String accountId,
        String serviceArea,
        double latitude,
        double longitude,
        Instant locationUpdatedAt,
        String vehicleId,
        String plateNumber,
        VehicleType vehicleType,
        int seats
) { }
