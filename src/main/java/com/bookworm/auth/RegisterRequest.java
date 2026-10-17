package com.bookworm.auth;

import com.bookworm.user.PasswordRules;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = PasswordRules.COMPLEXITY_REGEX, message = PasswordRules.COMPLEXITY_MESSAGE)
        String password) {

    /** Keeps the password out of logs and exception messages. */
    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + "]";
    }
}
