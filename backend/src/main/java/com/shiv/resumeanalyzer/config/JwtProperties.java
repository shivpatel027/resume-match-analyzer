package com.shiv.resumeanalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, @DefaultValue("PT2H") Duration expiration) {

    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("app.jwt.secret (JWT_SECRET) must be at least 32 characters");
        }
    }
}
