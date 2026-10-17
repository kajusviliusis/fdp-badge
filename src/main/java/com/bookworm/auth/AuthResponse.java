package com.bookworm.auth;

import com.bookworm.user.UserResponse;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {

    static AuthResponse bearer(IssuedToken token, UserResponse user) {
        return new AuthResponse(token.value(), "Bearer", token.expiresInSeconds(), user);
    }

    /** Keeps the token out of logs. */
    @Override
    public String toString() {
        return "AuthResponse[user=" + user + "]";
    }
}
