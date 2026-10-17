package com.bookworm.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 72) String password) {

    /** Keeps the password out of logs and exception messages. */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + "]";
    }
}
