package com.bookworm.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param secret         HMAC key used to sign access tokens (at least 32 bytes); supplied via the environment,
 *                       never committed
 * @param accessTokenTtl how long an access token stays valid
 * @param issuer         value of the {@code iss} claim, which incoming tokens must carry
 */
@ConfigurationProperties(prefix = "bookworm.security.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("PT1H") Duration accessTokenTtl,
        @DefaultValue("bookworm-api") String issuer) {

    /** Hides the signing key from {@code toString()}. */
    @Override
    public String toString() {
        return "JwtProperties[secret=redacted, accessTokenTtl=" + accessTokenTtl + ", issuer=" + issuer + "]";
    }
}
