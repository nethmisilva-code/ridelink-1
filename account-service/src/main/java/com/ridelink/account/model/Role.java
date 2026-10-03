package com.ridelink.account.model;

/**
 * Roles that can be self-registered. ADMIN is not a stored role: the single
 * administrator is configured through environment variables.
 */
public enum Role {
    PASSENGER,
    DRIVER,

    /**
     * Legacy name used by earlier builds of this service. Kept only so that
     * documents already stored with role "CUSTOMER" can still be loaded.
     * It is always treated as PASSENGER and is never issued in a token.
     */
    @Deprecated
    CUSTOMER;

    /** Maps the legacy CUSTOMER value onto PASSENGER. */
    public Role normalised() {
        return this == CUSTOMER ? PASSENGER : this;
    }
}
