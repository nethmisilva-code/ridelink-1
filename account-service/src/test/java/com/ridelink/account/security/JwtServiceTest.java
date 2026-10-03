package com.ridelink.account.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-chars";

    @Test
    void issuedToken_canBeParsedBackIntoTheSameIdentity() {
        JwtService jwt = new JwtService(SECRET, 30);

        AuthenticatedUser user = jwt.parse(jwt.issue("acc-1", "kasun@ridelink.test", "DRIVER"));

        assertThat(user.id()).isEqualTo("acc-1");
        assertThat(user.email()).isEqualTo("kasun@ridelink.test");
        assertThat(user.isDriver()).isTrue();
        assertThat(jwt.ttlSeconds()).isEqualTo(1800);
    }

    @Test
    void tokenSignedWithADifferentSecret_isRejected() {
        String foreign = new JwtService("another-secret-that-is-also-32-chars-long", 30).issue("acc-1", null, "ADMIN");

        assertThatThrownBy(() -> new JwtService(SECRET, 30).parse(foreign)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void tamperedToken_isRejected() {
        JwtService jwt = new JwtService(SECRET, 30);
        String token = jwt.issue("acc-1", null, "PASSENGER");
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThatThrownBy(() -> jwt.parse(tampered)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void expiredToken_isRejected() {
        Clock threeHoursAgo = Clock.fixed(Instant.now().minus(3, ChronoUnit.HOURS), ZoneOffset.UTC);
        String old = new JwtService(SECRET, 60, threeHoursAgo).issue("acc-1", null, "PASSENGER");

        assertThatThrownBy(() -> new JwtService(SECRET, 60).parse(old)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void garbageToken_isRejected() {
        assertThatThrownBy(() -> new JwtService(SECRET, 30).parse("not-a-jwt")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void missingOrShortSecret_failsFastWithAHelpfulMessage() {
        assertThatThrownBy(() -> new JwtService("", 30)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET is not set");
        assertThatThrownBy(() -> new JwtService("too-short", 30)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32");
    }
}
