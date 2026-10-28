package com.shiv.resumeanalyzer.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Limits how often each user can run a new analysis (the endpoint that calls the paid LLM API). */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final TokenBucketRateLimiter limiter;

    public RateLimitInterceptor(TokenBucketRateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String key = auth != null ? auth.getName() : request.getRemoteAddr();

        TokenBucketRateLimiter.Decision decision = limiter.tryConsume(key);
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        if (decision.allowed()) {
            return true;
        }

        long retrySeconds = Math.max(1, (long) Math.ceil(decision.retryAfter().toMillis() / 1000.0));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retrySeconds));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {"status":429,"title":"Too Many Requests","detail":"You've hit the analysis limit. Try again in %d seconds."}"""
                .formatted(retrySeconds));
        return false;
    }
}
