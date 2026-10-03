package com.ridelink.ride.security;

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
 * Verifies user tokens issued by Account Service, and signs the short-lived
 * SERVICE token this service presents when it calls Driver and Fare services.
 */
@Service
public class JwtService {

    public static final String SERVICE_NAME = "ride-management-service";
    private static final long CLOCK_SKEW_SECONDS = 30;

    private final SecretKey key;
    private final JwtParser parser;
    private final Clock clock;

    @Autowired
    public JwtService(@Value("${ridelink.jwt.secret}") String secret) {
        this(secret, Clock.systemUTC());
    }

    JwtService(String secret, Clock clock) {
        this.key = JwtKeys.fromSecret(secret);
        this.parser = Jwts.parser().verifyWith(key).clockSkewSeconds(CLOCK_SKEW_SECONDS).build();
        this.clock = clock;
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

    public String issueServiceToken(Duration ttl) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(SERVICE_NAME)
                .subject(SERVICE_NAME)
                .claim("role", AuthenticatedUser.SERVICE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }
}
