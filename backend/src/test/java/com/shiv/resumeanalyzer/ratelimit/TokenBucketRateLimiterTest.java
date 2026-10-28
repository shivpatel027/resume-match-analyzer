package com.shiv.resumeanalyzer.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBucketRateLimiterTest {

    /** A clock the test can move forward. */
    static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
        void advance(Duration d) { now = now.plus(d); }
    }

    @Test
    void allowsBurstUpToCapacityThenBlocks() {
        var limiter = new TokenBucketRateLimiter(3, Duration.ofMinutes(1), new MutableClock());

        assertThat(limiter.tryConsume("a").allowed()).isTrue();
        assertThat(limiter.tryConsume("a").allowed()).isTrue();
        assertThat(limiter.tryConsume("a").allowed()).isTrue();

        TokenBucketRateLimiter.Decision blocked = limiter.tryConsume("a");
        assertThat(blocked.allowed()).isFalse();
        // 3 per minute = one token every 20 seconds
        assertThat(blocked.retryAfter().toMillis()).isBetween(19_999L, 20_001L);
    }

    @Test
    void refillsOverTime() {
        var clock = new MutableClock();
        var limiter = new TokenBucketRateLimiter(2, Duration.ofMinutes(1), clock);
        limiter.tryConsume("a");
        limiter.tryConsume("a");
        assertThat(limiter.tryConsume("a").allowed()).isFalse();

        clock.advance(Duration.ofSeconds(31)); // 2 per minute = one token every 30s
        assertThat(limiter.tryConsume("a").allowed()).isTrue();
    }

    @Test
    void usersHaveSeparateBuckets() {
        var limiter = new TokenBucketRateLimiter(1, Duration.ofMinutes(1), new MutableClock());
        assertThat(limiter.tryConsume("alice").allowed()).isTrue();
        assertThat(limiter.tryConsume("bob").allowed()).isTrue();
        assertThat(limiter.tryConsume("alice").allowed()).isFalse();
    }
}
