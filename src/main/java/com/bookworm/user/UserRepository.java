package com.bookworm.user;

import com.bookworm.common.error.UnauthorizedException;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Loads the user that a request claims to act as.
     *
     * @throws UnauthorizedException if no such user exists
     */
    default User getAuthenticated(Long userId) {
        return findById(userId).orElseThrow(() -> new UnauthorizedException("Authentication is required."));
    }
}
