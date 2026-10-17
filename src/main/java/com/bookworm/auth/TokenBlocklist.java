package com.bookworm.auth;

import java.time.Instant;

/**
 * Remembers access tokens that were revoked before they expired (logout), so they can no longer be used.
 */
public interface TokenBlocklist {

    /**
     * @param tokenId   the token's {@code jti}
     * @param expiresAt when the token expires anyway; after that the entry is no longer needed
     */
    void block(String tokenId, Instant expiresAt);

    boolean isBlocked(String tokenId);
}
