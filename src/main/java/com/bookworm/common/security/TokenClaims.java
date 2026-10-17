package com.bookworm.common.security;

/** Names of the custom claims carried by the access tokens this application issues. */
public final class TokenClaims {

    /** The user's role names, e.g. {@code ["CUSTOMER"]}. */
    public static final String ROLES = "roles";

    private TokenClaims() {
    }
}
