package com.ridelink.driver.service;

import com.ridelink.driver.exception.ForbiddenOperationException;
import com.ridelink.driver.model.Driver;
import com.ridelink.driver.security.AuthenticatedUser;

/**
 * Ownership rule shared by DriverService and VehicleService: a driver may only
 * change their own profile and vehicles. ADMIN may change anything, and the
 * SERVICE role (Ride Management Service) may change availability.
 */
final class OwnershipGuard {

    private OwnershipGuard() {
    }

    static void assertCanManage(Driver driver, AuthenticatedUser caller, boolean allowService) {
        if (caller.isAdmin() || (allowService && caller.isService())) {
            return;
        }
        if (caller.isDriver() && caller.id().equals(driver.getAccountId())) {
            return;
        }
        throw new ForbiddenOperationException("You can only manage your own driver profile and vehicles");
    }
}
