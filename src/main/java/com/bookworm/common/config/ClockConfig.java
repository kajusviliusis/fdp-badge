package com.bookworm.common.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    /** Injected wherever the current time matters so that time-dependent rules can be tested. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
