package com.shiv.resumeanalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("app.ai")
public record AiProperties(
        String apiKey,
        @DefaultValue("claude-haiku-5-5") String model,
        @DefaultValue("https://api.anthropic.com") String baseUrl,
        @DefaultValue("2000") int maxTokens,
        @DefaultValue("PT30S") Duration timeout,
        @DefaultValue("15000") int maxResumeChars) {

    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
