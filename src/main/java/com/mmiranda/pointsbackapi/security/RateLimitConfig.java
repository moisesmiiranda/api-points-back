package com.mmiranda.pointsbackapi.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class RateLimitConfig {

    /**
     * Throttle for "forgot password" requests, keyed like the login throttle (IP + email). Every request
     * counts, whether or not the email exists, so the 429 reveals nothing about which emails are registered.
     */
    @Bean
    public LoginRateLimiter passwordResetRateLimiter(
            @Value("${app.password-reset.max-requests:5}") int maxRequests,
            @Value("${app.password-reset.window-minutes:15}") long windowMinutes) {
        return new LoginRateLimiter(maxRequests, Duration.ofMinutes(windowMinutes), Clock.systemUTC());
    }
}
