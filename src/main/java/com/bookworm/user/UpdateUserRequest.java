package com.bookworm.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Partial update: only the fields that are present are changed.
 */
public record UpdateUserRequest(
        @Size(min = 2, max = 100) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String name,
        @Email @Size(max = 255) String email,
        @Size(min = 8, max = 72)
        @Pattern(regexp = PasswordRules.COMPLEXITY_REGEX, message = PasswordRules.COMPLEXITY_MESSAGE)
        String password) {

    /** Keeps the password out of logs and exception messages. */
    @Override
    public String toString() {
        return "UpdateUserRequest[name=" + name + ", email=" + email + "]";
    }
}
