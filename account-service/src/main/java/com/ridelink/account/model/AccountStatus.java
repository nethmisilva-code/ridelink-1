package com.ridelink.account.model;

/**
 * PASSENGER accounts start ACTIVE. DRIVER accounts start PENDING and must be
 * approved (set to ACTIVE) by an administrator before a driver profile can be
 * created. INACTIVE accounts cannot log in.
 */
public enum AccountStatus {
    ACTIVE,
    PENDING,
    INACTIVE
}
