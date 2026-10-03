package com.ridelink.driver.repository;

import java.time.Instant;

/** Operations that need a single atomic MongoDB update instead of read-modify-write. */
public interface DriverRepositoryCustom {

    /**
     * Atomically changes the driver from AVAILABLE to ON_TRIP.
     *
     * @return true if this call reserved the driver, false if the driver was no
     *         longer AVAILABLE (for example, another ride reserved them first)
     */
    boolean reserveIfAvailable(String driverId, Instant now);
}
