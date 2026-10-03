package com.ridelink.driver.model;

/**
 * OFFLINE: not accepting rides. AVAILABLE: can be matched to a ride.
 * ON_TRIP: reserved by Ride Management Service for an assigned ride.
 */
public enum AvailabilityStatus {
    OFFLINE,
    AVAILABLE,
    ON_TRIP
}
