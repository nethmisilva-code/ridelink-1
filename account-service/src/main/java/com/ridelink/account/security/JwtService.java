package com.ridelink.account.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

/**
 * Issues and verifies RideLink user tokens.
 * Claims: sub = account id, email, role (PASSENGER, DRIVER or ADMIN), iss, iat, exp.
 */
@Service
public class JwtService {

    public static final String ISSUER = "ridelink-account-service";
    private static final long CLOCK_SKEW_SECONDS = 30;

    private final SecretKey key;
    private final JwtParser parser;
    private final Duration ttl;
    private final Clock clock;

    @Autowired
    public JwtService(@Value("${ridelink.jwt.secret}") String secret,
            @Value("${ridelink.jwt.ttl-minutes:120}") long ttlMinutes) {
        this(secret, ttlMinutes, Clock.systemUTC());
    }

    JwtService(String secret, long ttlMinutes, Clock clock) {
        if (ttlMinutes < 1) {
            throw new IllegalStateException("JWT_TTL_MINUTES must be at least 1");
        }
        this.key = JwtKeys.fromSecret(secret);
        this.parser = Jwts.parser().verifyWith(key).clockSkewSeconds(CLOCK_SKEW_SECONDS).build();
        this.ttl = Duration.ofMinutes(ttlMinutes);
        this.clock = clock;
    }

    public String issue(String subject, String email, String role) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(subject)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public long ttlSeconds() {
        return ttl.toSeconds();
    }

    public AuthenticatedUser parse(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            String subject = claims.getSubject();
            String role = claims.get("role", String.class);
            if (subject == null || subject.isBlank() || role == null || role.isBlank()) {
                throw new InvalidTokenException("Token is missing the subject or role claim");
            }
            return new AuthenticatedUser(subject, claims.get("email", String.class), role);
        }
        catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid or expired token", ex);
        }
    }
}
