package com.shiv.resumeanalyzer.config;

import com.shiv.resumeanalyzer.ratelimit.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Only the AI-backed endpoint is rate limited; the interceptor itself checks for POST.
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/analyses");
    }
}
