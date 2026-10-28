package com.shiv.resumeanalyzer.security;

import com.shiv.resumeanalyzer.config.JwtProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";

    @Test
    void roundTripsSubject() {
        JwtService jwt = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1)));
        assertThat(jwt.validateAndGetSubject(jwt.generateToken("shiv@example.com"))).contains("shiv@example.com");
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        JwtService other = new JwtService(new JwtProperties("another-secret-another-secret-123456", Duration.ofHours(1)));
        JwtService jwt = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1)));
        assertThat(jwt.validateAndGetSubject(other.generateToken("x@example.com"))).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        JwtService jwt = new JwtService(new JwtProperties(SECRET, Duration.ofSeconds(-5)));
        assertThat(jwt.validateAndGetSubject(jwt.generateToken("x@example.com"))).isEmpty();
    }

    @Test
    void refusesShortSecret() {
        assertThatThrownBy(() -> new JwtProperties("too-short", Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
