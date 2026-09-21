package com.mmiranda.pointsbackapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Clock;

/** Async email sending, and a Clock bean so time-dependent logic can be tested with a fixed clock. */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
