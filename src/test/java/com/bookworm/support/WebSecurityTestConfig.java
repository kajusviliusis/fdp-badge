package com.bookworm.support;

import static org.mockito.Mockito.mock;

import com.bookworm.common.security.SecurityConfig;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Applies the application's real security rules to a {@code @WebMvcTest} slice. Tokens are injected with
 * {@link TestAuth}, so the decoder only needs to exist.
 */
@TestConfiguration
@Import(SecurityConfig.class)
public class WebSecurityTestConfig {

    @Bean
    JwtDecoder jwtDecoder() {
        return mock(JwtDecoder.class);
    }
}
