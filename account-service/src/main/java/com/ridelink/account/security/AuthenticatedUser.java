package com.ridelink.account.security;

import java.security.Principal;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

/**
 * The caller identity extracted from a verified JWT.
 *
 * @param id    account id for users, or the calling service name for SERVICE tokens
 * @param email email address (null for SERVICE tokens)
 * @param role  PASSENGER, DRIVER, ADMIN or SERVICE
 */
public record AuthenticatedUser(String id, String email, String role) implements Principal {

    public static final String PASSENGER = "PASSENGER";
    public static final String DRIVER = "DRIVER";
    public static final String ADMIN = "ADMIN";
    public static final String SERVICE = "SERVICE";

    public AuthenticatedUser {
        role = role == null ? null : role.toUpperCase(Locale.ROOT);
    }

    @Override
    public String getName() {
        return id;
    }

    public boolean hasRole(String expected) {
        return expected.equalsIgnoreCase(role);
    }

    public boolean isPassenger() {
        return hasRole(PASSENGER);
    }

    public boolean isDriver() {
        return hasRole(DRIVER);
    }

    public boolean isAdmin() {
        return hasRole(ADMIN);
    }

    public boolean isService() {
        return hasRole(SERVICE);
    }

    /** Admins and trusted internal services bypass ownership checks. */
    public boolean isPrivileged() {
        return isAdmin() || isService();
    }

    public static AuthenticatedUser from(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
}
