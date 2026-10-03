package com.ridelink.driver.security;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

/** Verifies tokens issued by Account Service (users) or by Ride Management Service (SERVICE role). */
@Service
public class JwtService {

    private static final long CLOCK_SKEW_SECONDS = 30;

    private final JwtParser parser;

    public JwtService(@Value("${ridelink.jwt.secret}") String secret) {
        SecretKey key = JwtKeys.fromSecret(secret);
        this.parser = Jwts.parser().verifyWith(key).clockSkewSeconds(CLOCK_SKEW_SECONDS).build();
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
