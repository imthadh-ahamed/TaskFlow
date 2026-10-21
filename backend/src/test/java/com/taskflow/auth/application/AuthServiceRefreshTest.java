package com.taskflow.auth.application;

import com.taskflow.auth.api.AuthResponse;
import com.taskflow.auth.domain.RefreshTokenRecord;
import com.taskflow.auth.domain.UserAccount;
import com.taskflow.auth.infrastructure.RefreshTokenRepository;
import com.taskflow.auth.infrastructure.UserAccountRepository;
import com.taskflow.common.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshTest {

    private static final String SECRET = "test-secret-value-that-is-long-enough-for-hs256-algorithm";

    @Mock UserAccountRepository userRepo;
    @Mock RefreshTokenRepository refreshTokenRepo;
    @Mock CookieService cookieService;
    @Mock PasswordEncoder passwordEncoder;

    JwtService jwtService;
    AuthService authService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 15, 7);
        authService = new AuthService(userRepo, refreshTokenRepo, jwtService, cookieService, passwordEncoder);
    }

    // ---- refresh ----

    @Test
    void refresh_validToken_rotatesAndReturnsNewCookies() {
        JwtService.TokenPair original = jwtService.generateTokenPair(1L);
        UUID origJti = original.refreshJti();

        RefreshTokenRecord record = new RefreshTokenRecord(
                origJti, UUID.randomUUID(), 1L, false, null,
                Instant.now().plusSeconds(604800));

        UserAccount user = new UserAccount(1L, "alice@example.com", "Alice", "hash", "user", true);

        when(refreshTokenRepo.findByJtiForUpdate(origJti)).thenReturn(Optional.of(record));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(cookieService.buildAccessCookie(any())).thenReturn(
                org.springframework.http.ResponseCookie.from("tf_access", "new-access").build());
        when(cookieService.buildRefreshCookie(any())).thenReturn(
                org.springframework.http.ResponseCookie.from("tf_refresh", "new-refresh").build());

        LoginResult result = authService.refresh(original.refreshToken());

        assertThat(result.response().email()).isEqualTo("alice@example.com");
        verify(refreshTokenRepo).save(any(), eq(record.familyId()), eq(1L), any());
        verify(refreshTokenRepo).revokeAndReplace(eq(origJti), any());
    }

    @Test
    void refresh_revokedToken_revokesFamilyAndThrows() {
        JwtService.TokenPair original = jwtService.generateTokenPair(2L);
        UUID jti = original.refreshJti();
        UUID familyId = UUID.randomUUID();

        RefreshTokenRecord revoked = new RefreshTokenRecord(
                jti, familyId, 2L, true, UUID.randomUUID(),
                Instant.now().plusSeconds(604800));

        when(refreshTokenRepo.findByJtiForUpdate(jti)).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> authService.refresh(original.refreshToken()))
                .isInstanceOf(SessionRevokedException.class);

        verify(refreshTokenRepo).revokeFamily(familyId);
        verify(refreshTokenRepo, never()).save(any(), any(), anyLong(), any());
    }

    @Test
    void refresh_expiredToken_throwsTokenValidationException() {
        JwtService shortLived = new JwtService(SECRET, 0, 0);
        JwtService.TokenPair expired = shortLived.generateTokenPair(3L);

        assertThatThrownBy(() -> authService.refresh(expired.refreshToken()))
                .isInstanceOf(TokenValidationException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void refresh_unknownJti_throws() {
        JwtService.TokenPair pair = jwtService.generateTokenPair(4L);
        when(refreshTokenRepo.findByJtiForUpdate(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(pair.refreshToken()))
                .isInstanceOf(TokenValidationException.class);
    }

    @Test
    void refresh_accessTokenRejected() {
        JwtService.TokenPair pair = jwtService.generateTokenPair(5L);

        assertThatThrownBy(() -> authService.refresh(pair.accessToken()))
                .isInstanceOf(TokenValidationException.class)
                .hasMessageContaining("Wrong token type");
    }

    // ---- logout ----

    @Test
    void logout_validToken_revokesRecord() {
        JwtService.TokenPair pair = jwtService.generateTokenPair(1L);
        UUID jti = pair.refreshJti();

        RefreshTokenRecord record = new RefreshTokenRecord(
                jti, UUID.randomUUID(), 1L, false, null,
                Instant.now().plusSeconds(604800));

        when(refreshTokenRepo.findByJtiForUpdate(jti)).thenReturn(Optional.of(record));

        authService.logout(pair.refreshToken());

        verify(refreshTokenRepo).revokeByJti(jti);
    }

    @Test
    void logout_expiredToken_noDbInteraction() {
        JwtService shortLived = new JwtService(SECRET, 0, 0);
        JwtService.TokenPair pair = shortLived.generateTokenPair(1L);

        authService.logout(pair.refreshToken());

        verifyNoInteractions(refreshTokenRepo);
    }

    @Test
    void logout_nullToken_noDbInteraction() {
        authService.logout(null);
        verifyNoInteractions(refreshTokenRepo);
    }

    @Test
    void logout_alreadyRevokedToken_doesNotDoubleRevoke() {
        JwtService.TokenPair pair = jwtService.generateTokenPair(1L);
        UUID jti = pair.refreshJti();

        RefreshTokenRecord alreadyRevoked = new RefreshTokenRecord(
                jti, UUID.randomUUID(), 1L, true, UUID.randomUUID(),
                Instant.now().plusSeconds(604800));

        when(refreshTokenRepo.findByJtiForUpdate(jti)).thenReturn(Optional.of(alreadyRevoked));

        authService.logout(pair.refreshToken());

        verify(refreshTokenRepo, never()).revokeByJti(any());
    }

    // ---- me ----

    @Test
    void me_returnsCurrentUserProfile() {
        UserAccount user = new UserAccount(7L, "bob@example.com", "Bob", "hash", "user", true);
        when(userRepo.findById(7L)).thenReturn(Optional.of(user));

        AuthResponse resp = authService.me(7L);

        assertThat(resp.email()).isEqualTo("bob@example.com");
        assertThat(resp.id()).isEqualTo(7L);
    }

    @Test
    void me_inactiveUser_throws() {
        UserAccount inactive = new UserAccount(8L, "x@example.com", "X", "hash", "user", false);
        when(userRepo.findById(8L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> authService.me(8L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void me_unknownUser_throws() {
        when(userRepo.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me(9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
