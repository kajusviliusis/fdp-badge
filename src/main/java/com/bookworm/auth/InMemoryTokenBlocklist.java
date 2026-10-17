package com.bookworm.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Keeps revoked token ids in memory. Entries are dropped once the token would have expired anyway, so the set
 * stays small. Revocations are lost on restart and are not shared between instances; swap in a shared store
 * behind {@link TokenBlocklist} if the application is ever scaled out.
 */
@Component
public class InMemoryTokenBlocklist implements TokenBlocklist {

    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryTokenBlocklist(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void block(String tokenId, Instant expiresAt) {
        Instant now = clock.instant();
        revoked.values().removeIf(expiry -> !expiry.isAfter(now));
        revoked.put(tokenId, expiresAt);
    }

    @Override
    public boolean isBlocked(String tokenId) {
        return revoked.containsKey(tokenId);
    }
}
