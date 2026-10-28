package com.shiv.resumeanalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** Each user gets {@code capacity} analyses, refilled evenly over {@code refillPeriod}. */
@ConfigurationProperties("app.rate-limit")
public record RateLimitProperties(
        @DefaultValue("5") int capacity,
        @DefaultValue("PT1M") Duration refillPeriod) {
}
