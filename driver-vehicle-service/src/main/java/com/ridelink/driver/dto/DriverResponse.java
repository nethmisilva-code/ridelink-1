package com.ridelink.driver.dto;

import com.ridelink.driver.model.AvailabilityStatus;
import com.ridelink.driver.model.Driver;

import java.time.Instant;

public record DriverResponse(
        String id,
        String accountId,
        String licenseNumber,
        String serviceArea,
        AvailabilityStatus availability,
        Double latitude,
        Double longitude,
        Instant locationUpdatedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static DriverResponse from(Driver d) {
        return new DriverResponse(
                d.getId(),
                d.getAccountId(),
                d.getLicenseNumber(),
                d.getServiceArea(),
                d.getAvailability(),
                d.getCurrentLocation() == null ? null : d.getCurrentLocation().getLatitude(),
                d.getCurrentLocation() == null ? null : d.getCurrentLocation().getLongitude(),
                d.getLocationUpdatedAt(),
                d.getCreatedAt(),
                d.getUpdatedAt());
    }
}
