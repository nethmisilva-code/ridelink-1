package com.ridelink.ride.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import io.jsonwebtoken.security.Keys;

/**
 * Builds the shared HMAC key from JWT_SECRET. Every RideLink service derives
 * the key in exactly the same way (raw UTF-8 bytes of the secret), which is
 * what allows a token issued by Account Service to be verified everywhere.
 */
public final class JwtKeys {

    public static final int MIN_SECRET_BYTES = 32;

    private JwtKeys() {
    }

    public static SecretKey fromSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is not set. Copy .env.example to .env in the project root "
                    + "(see README) or set the JWT_SECRET environment variable.");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least " + MIN_SECRET_BYTES
                    + " characters long (256 bits) for HMAC-SHA256.");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
