package com.bookworm.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class AuthConfigTest {

    private static final Instant NOW = Instant.parse("2025-03-01T12:00:00Z");

    private final AuthConfig config = new AuthConfig();

    private static JwtProperties properties(String secret) {
        return new JwtProperties(secret, Duration.ofHours(1), "bookworm-api");
    }

    @Test
    void missingSecretFailsStartup() {
        assertThatThrownBy(() -> config.jwtSigningKey(properties("")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> config.jwtSigningKey(properties(null))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shortSecretFailsStartup() {
        assertThatThrownBy(() -> config.jwtSigningKey(properties("too-short")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void secretIsNeverPrinted() {
        assertThat(properties("super-secret-value-super-secret-value").toString()).doesNotContain("super-secret");
    }

    @Test
    void blocklistForgetsEntriesOnceTheyWouldHaveExpired() {
        InMemoryTokenBlocklist blocklist = new InMemoryTokenBlocklist(Clock.fixed(NOW, ZoneOffset.UTC));
        blocklist.block("old", NOW.minusSeconds(1));
        blocklist.block("current", NOW.plusSeconds(60));

        assertThat(blocklist.isBlocked("current")).isTrue();
        assertThat(blocklist.isBlocked("unknown")).isFalse();
    }

    @Test
    void decoderRejectsTokensFromAnotherIssuer() {
        SecretKey key = config.jwtSigningKey(properties("test-only-secret-that-is-at-least-32-bytes-long"));
        JwtDecoder decoder = config.jwtDecoder(key, properties("test-only-secret-that-is-at-least-32-bytes-long"),
                new InMemoryTokenBlocklist(Clock.fixed(NOW, ZoneOffset.UTC)));
        TokenService otherIssuer = new TokenService(config.jwtEncoder(key),
                new JwtProperties("test-only-secret-that-is-at-least-32-bytes-long", Duration.ofHours(1), "someone-else"),
                Clock.systemUTC());

        String token = otherIssuer.issueFor(newUser()).value();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
    }

    private static com.bookworm.user.User newUser() {
        com.bookworm.user.User user = new com.bookworm.user.User("A B", "a@example.com", "hash");
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}
