package com.bookworm.user;

import com.bookworm.common.error.ConflictException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile management for the authenticated user.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse getProfile(Long userId) {
        return UserResponse.from(userRepository.getAuthenticated(userId));
    }

    public GiftPointsResponse getGiftPoints(Long userId) {
        return new GiftPointsResponse(userId, userRepository.getAuthenticated(userId).getGiftPoints());
    }

    /**
     * Applies the fields present in the request.
     *
     * @throws ConflictException if the new email belongs to another account
     */
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateUserRequest request) {
        User user = userRepository.getAuthenticated(userId);
        if (request.name() != null) {
            user.rename(request.name().strip());
        }
        if (request.email() != null) {
            changeEmail(user, request.email());
        }
        if (request.password() != null) {
            user.changePasswordHash(passwordEncoder.encode(request.password()));
        }
        return UserResponse.from(user);
    }

    private void changeEmail(User user, String requested) {
        String email = User.normalizeEmail(requested);
        if (email.equals(user.getEmail())) {
            return;
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists.");
        }
        user.changeEmail(email);
    }
}
