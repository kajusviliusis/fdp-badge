package com.bookworm.user;

import java.util.Optional;

import com.bookworm.common.error.UnauthorizedException;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    /** @param email an address already passed through {@link User#normalizeEmail} */
    Optional<User> findByEmail(String email);

    /** @param email an address already passed through {@link User#normalizeEmail} */
    boolean existsByEmail(String email);

    /**
     * Loads the user that a request claims to act as.
     *
     * @throws UnauthorizedException if no such user exists
     */
    default User getAuthenticated(Long userId) {
        return findById(userId).orElseThrow(() -> new UnauthorizedException("Authentication is required."));
    }
}
