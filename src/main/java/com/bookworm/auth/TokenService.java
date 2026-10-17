package com.bookworm.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.bookworm.common.security.TokenClaims;
import com.bookworm.user.User;

import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Issues signed access tokens. The subject is the user id; the role travels in the {@link TokenClaims#ROLES}
 * claim so requests can be authorised without a database lookup.
 */
@Service
class TokenService {

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    TokenService(JwtEncoder encoder, JwtProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    IssuedToken issueFor(User user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .id(UUID.randomUUID().toString())
                .claim(TokenClaims.ROLES, List.of(user.getRole().name()))
                .build();
        String token = encoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(AuthConfig.SIGNING_ALGORITHM).build(), claims)).getTokenValue();
        return new IssuedToken(token, properties.accessTokenTtl().toSeconds());
    }
}
