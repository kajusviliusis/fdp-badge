package com.bookworm.auth;

import com.bookworm.common.error.ConflictException;
import com.bookworm.common.error.UnauthorizedException;
import com.bookworm.user.User;
import com.bookworm.user.UserRepository;
import com.bookworm.user.UserResponse;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration, login and logout.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final TokenBlocklist tokenBlocklist;

    /** Checked against when the email is unknown, so that response time does not reveal which emails exist. */
    private final String unknownUserHash;

    AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService,
                TokenBlocklist tokenBlocklist) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.tokenBlocklist = tokenBlocklist;
        this.unknownUserHash = passwordEncoder.encode("unknown-user-placeholder");
    }

    /**
     * Creates a customer account and signs it in.
     *
     * @throws ConflictException if the email is already registered
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists.");
        }
        User user = userRepository.saveAndFlush(
                new User(request.name().strip(), email, passwordEncoder.encode(request.password())));
        return AuthResponse.bearer(tokenService.issueFor(user), UserResponse.from(user));
    }

    /**
     * @throws UnauthorizedException if the credentials are wrong; the message never says which part was
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(User.normalizeEmail(request.email())).orElse(null);
        String hash = user == null ? unknownUserHash : user.getPasswordHash();
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user == null || !matches) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        return AuthResponse.bearer(tokenService.issueFor(user), UserResponse.from(user));
    }

    /** Revokes the presented token for the rest of its lifetime. */
    public void logout(Jwt token) {
        tokenBlocklist.block(token.getId(), token.getExpiresAt());
    }
}
