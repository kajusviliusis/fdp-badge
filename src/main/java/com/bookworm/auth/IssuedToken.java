package com.bookworm.auth;

/**
 * @param value            the signed JWT
 * @param expiresInSeconds validity from now, as reported to clients
 */
record IssuedToken(String value, long expiresInSeconds) {
}
