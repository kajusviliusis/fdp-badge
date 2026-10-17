package com.bookworm.user;

/**
 * Shared password rules for registration and profile updates.
 */
public final class PasswordRules {

    /**
     * At least one uppercase letter, one digit and one special character. Restricted to printable ASCII so the
     * password never exceeds BCrypt's 72-byte input limit.
     */
    public static final String COMPLEXITY_REGEX = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])[\\x20-\\x7E]*$";

    public static final String COMPLEXITY_MESSAGE =
            "must contain an uppercase letter, a digit and a special character, using printable ASCII characters only";

    private PasswordRules() {
    }
}
