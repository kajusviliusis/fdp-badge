package com.bookworm.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Authenticates a MockMvc request as a given user, as the real JWT filter would.
 */
public final class TestAuth {

    private TestAuth() {
    }

    /** A signed-in customer whose token subject is {@code userId}. */
    public static RequestPostProcessor user(long userId) {
        return jwt().jwt(token -> token.subject(String.valueOf(userId)))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    /** A signed-in administrator. */
    public static RequestPostProcessor admin(long userId) {
        return jwt().jwt(token -> token.subject(String.valueOf(userId)))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
