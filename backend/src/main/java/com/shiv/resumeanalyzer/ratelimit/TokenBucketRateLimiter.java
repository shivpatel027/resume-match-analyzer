package com.shiv.resumeanalyzer.ratelimit;

import com.shiv.resumeanalyzer.config.RateLimitProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token bucket per key (the user's email). Each bucket holds up to {@code capacity}
 * tokens and refills continuously at capacity / refillPeriod. Fine for a single instance; with
 * several instances behind a load balancer this would move to Redis.
 */
@Component
public class TokenBucketRateLimiter {

    private final int capacity;
    private final double tokensPerNano;
    private final Clock clock;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Autowired
    public TokenBucketRateLimiter(RateLimitProperties properties) {
        this(properties.capacity(), properties.refillPeriod(), Clock.systemUTC());
    }

    TokenBucketRateLimiter(int capacity, Duration refillPeriod, Clock clock) {
        if (capacity < 1 || refillPeriod.isZero() || refillPeriod.isNegative()) {
            throw new IllegalArgumentException("capacity must be >= 1 and refillPeriod positive");
        }
        this.capacity = capacity;
        this.tokensPerNano = (double) capacity / refillPeriod.toNanos();
        this.clock = clock;
    }

    /** Takes one token for {@code key}. Returns how long to wait if none is available. */
    public Decision tryConsume(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity, nanosNow()));
        synchronized (bucket) {
            long now = nanosNow();
            bucket.tokens = Math.min(capacity, bucket.tokens + (now - bucket.lastRefillNanos) * tokensPerNano);
            bucket.lastRefillNanos = now;
            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
                return new Decision(true, (int) Math.floor(bucket.tokens), Duration.ZERO);
            }
            long waitNanos = (long) Math.ceil((1 - bucket.tokens) / tokensPerNano);
            return new Decision(false, 0, Duration.ofNanos(waitNanos));
        }
    }

    private long nanosNow() {
        return Duration.ofMillis(clock.millis()).toNanos();
    }

    public record Decision(boolean allowed, int remaining, Duration retryAfter) {
    }

    private static final class Bucket {
        double tokens;
        long lastRefillNanos;

        Bucket(double tokens, long lastRefillNanos) {
            this.tokens = tokens;
            this.lastRefillNanos = lastRefillNanos;
        }
    }
}
