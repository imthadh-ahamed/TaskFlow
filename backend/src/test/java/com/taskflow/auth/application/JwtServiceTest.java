package com.taskflow.auth.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // 52 chars = 416 bits — satisfies JJWT's HS256 minimum (256 bits).
    private static final String SECRET = "test-secret-value-that-is-long-enough-for-hs256-algorithm";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 15, 7);
    }

    @Test
    void generateTokenPair_producesNonNullTokensAndIds() {
        JwtService.TokenPair pair = jwtService.generateTokenPair(42L);

        assertThat(pair.accessToken()).isNotBlank();
        assertThat(pair.refreshToken()).isNotBlank();
        assertThat(pair.accessJti()).isNotNull();
        assertThat(pair.refreshJti()).isNotNull();
        assertThat(pair.familyId()).isNotNull();
        assertThat(pair.refreshExpiry()).isNotNull();
        assertThat(pair.accessToken()).isNotEqualTo(pair.refreshToken());
    }

    @Test
    void validateAccessToken_returnsCorrectUserIdAndJti() {
        JwtService.TokenPair pair = jwtService.generateTokenPair(99L);

        JwtService.ParsedClaims claims = jwtService.validateAccessToken(pair.accessToken());

        assertThat(claims.userId()).isEqualTo(99L);
        assertThat(claims.jti()).isEqualTo(pair.accessJti().toString());
    }

    @Test
    void validateRefreshToken_returnsCorrectUserIdAndJti() {
        JwtService.TokenPair pair = jwtService.generateTokenPair(7L);

        JwtService.ParsedClaims claims = jwtService.validateRefreshToken(pair.refreshToken());

        assertThat(claims.userId()).isEqualTo(7L);
        assertThat(claims.jti()).isEqualTo(pair.refreshJti().toString());
    }

    @Test
    void validateAccessToken_rejectsRefreshToken() {
        JwtService.TokenPair pair = jwtService.generateTokenPair(1L);

        assertThatThrownBy(() -> jwtService.validateAccessToken(pair.refreshToken()))
                .isInstanceOf(TokenValidationException.class)
                .hasMessageContaining("Wrong token type");
    }

    @Test
    void validateRefreshToken_rejectsAccessToken() {
        JwtService.TokenPair pair = jwtService.generateTokenPair(1L);

        assertThatThrownBy(() -> jwtService.validateRefreshToken(pair.accessToken()))
                .isInstanceOf(TokenValidationException.class)
                .hasMessageContaining("Wrong token type");
    }

    @Test
    void validateAccessToken_rejectsExpiredToken() {
        // TTL = 0 minutes → expires immediately
        JwtService shortLived = new JwtService(SECRET, 0, 7);
        JwtService.TokenPair pair = shortLived.generateTokenPair(5L);

        assertThatThrownBy(() -> jwtService.validateAccessToken(pair.accessToken()))
                .isInstanceOf(TokenValidationException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void validateAccessToken_rejectsWrongSignature() {
        JwtService other = new JwtService("completely-different-secret-value-long-enough-hs256", 15, 7);
        JwtService.TokenPair pair = other.generateTokenPair(3L);

        assertThatThrownBy(() -> jwtService.validateAccessToken(pair.accessToken()))
                .isInstanceOf(TokenValidationException.class);
    }

    @Test
    void generateTokenPair_differentCallsProduceDifferentJtis() {
        JwtService.TokenPair a = jwtService.generateTokenPair(1L);
        JwtService.TokenPair b = jwtService.generateTokenPair(1L);

        assertThat(a.accessJti()).isNotEqualTo(b.accessJti());
        assertThat(a.familyId()).isNotEqualTo(b.familyId());
    }
}
