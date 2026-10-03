package com.ridelink.ride.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

/**
 * Supplies "Bearer &lt;token&gt;" for service-to-service calls. The token has the
 * SERVICE role, lives 5 minutes and is reused until one minute before expiry.
 */
@Component
public class ServiceTokenProvider {

    static final Duration TTL = Duration.ofMinutes(5);
    private static final Duration RENEW_BEFORE_EXPIRY = Duration.ofMinutes(1);

    private final JwtService jwtService;
    private final Clock clock;
    private String token;
    private Instant expiresAt = Instant.EPOCH;

    public ServiceTokenProvider(JwtService jwtService, Clock clock) {
        this.jwtService = jwtService;
        this.clock = clock;
    }

    public synchronized String bearerHeader() {
        Instant now = clock.instant();
        if (token == null || !now.isBefore(expiresAt.minus(RENEW_BEFORE_EXPIRY))) {
            token = jwtService.issueServiceToken(TTL);
            expiresAt = now.plus(TTL);
        }
        return "Bearer " + token;
    }
}
