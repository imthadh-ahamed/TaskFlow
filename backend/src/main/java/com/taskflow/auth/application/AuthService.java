package com.taskflow.auth.application;

import com.taskflow.auth.api.AuthResponse;
import com.taskflow.auth.api.LoginRequest;
import com.taskflow.auth.api.RegisterRequest;
import com.taskflow.auth.domain.RefreshTokenRecord;
import com.taskflow.auth.domain.UserAccount;
import com.taskflow.auth.infrastructure.RefreshTokenRepository;
import com.taskflow.auth.infrastructure.UserAccountRepository;
import com.taskflow.common.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserAccountRepository userRepo;
    private final RefreshTokenRepository refreshTokenRepo;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final PasswordEncoder passwordEncoder;

    AuthService(
            UserAccountRepository userRepo,
            RefreshTokenRepository refreshTokenRepo,
            JwtService jwtService,
            CookieService cookieService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepo = userRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.jwtService = jwtService;
        this.cookieService = cookieService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a new account. Does not issue tokens; the user must log in separately.
     * Email is normalized to lowercase before persistence.
     *
     * @throws DuplicateEmailException if the email is already registered
     */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().strip().toLowerCase();
        if (userRepo.findByEmail(email).isPresent()) {
            throw new DuplicateEmailException("Email already in use");
        }
        String hash = passwordEncoder.encode(req.password());
        UserAccount user = userRepo.save(email, req.displayName().strip(), hash);
        return toResponse(user);
    }

    /**
     * Authenticates a user and issues an access + refresh cookie pair.
     * The same generic error is returned for unknown email and wrong password to prevent enumeration.
     *
     * @throws InvalidCredentialsException for any authentication failure
     */
    @Transactional
    public LoginResult login(LoginRequest req) {
        String email = req.email().strip().toLowerCase();

        UserAccount user = userRepo.findByEmail(email)
                .filter(UserAccount::active)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(req.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        JwtService.TokenPair pair = jwtService.generateTokenPair(user.id());
        refreshTokenRepo.save(pair.refreshJti(), pair.familyId(), user.id(), pair.refreshExpiry());

        return new LoginResult(
                toResponse(user),
                cookieService.buildAccessCookie(pair.accessToken()),
                cookieService.buildRefreshCookie(pair.refreshToken())
        );
    }

    /**
     * Rotates a refresh token: validates the presented token, issues a new token pair,
     * revokes the old token, and records the rotation chain.
     *
     * <p>Concurrency: {@code findByJtiForUpdate} holds a row-level lock for the duration of the
     * transaction. A second concurrent request for the same token will block until the first
     * commits, then observe the token already revoked and trigger family revocation.
     *
     * @throws TokenValidationException   if the JWT is expired, malformed, or wrong type
     * @throws SessionRevokedException    if the token has been revoked (possible replay attack;
     *                                    the entire token family is revoked before throwing)
     */
    @Transactional
    public LoginResult refresh(String refreshTokenValue) {
        JwtService.ParsedClaims claims = jwtService.validateRefreshToken(refreshTokenValue);
        UUID jti = UUID.fromString(claims.jti());

        RefreshTokenRecord record = refreshTokenRepo.findByJtiForUpdate(jti)
                .orElseThrow(() -> new TokenValidationException("Unknown refresh token"));

        if (record.revoked()) {
            // Possible replay: revoke the entire family to invalidate all sibling sessions.
            refreshTokenRepo.revokeFamily(record.familyId());
            log.warn("Refresh token reuse detected for family={}", record.familyId());
            throw new SessionRevokedException();
        }

        JwtService.TokenPair newPair = jwtService.generateTokenPair(record.userId());
        refreshTokenRepo.save(newPair.refreshJti(), record.familyId(), record.userId(), newPair.refreshExpiry());
        refreshTokenRepo.revokeAndReplace(jti, newPair.refreshJti());

        UserAccount user = userRepo.findById(record.userId())
                .filter(UserAccount::active)
                .orElseThrow(() -> new TokenValidationException("User account not found or inactive"));

        return new LoginResult(
                toResponse(user),
                cookieService.buildAccessCookie(newPair.accessToken()),
                cookieService.buildRefreshCookie(newPair.refreshToken())
        );
    }

    /**
     * Revokes the active refresh token session and clears both cookies.
     * If the token is missing, expired, or invalid the logout still succeeds — cookies are cleared.
     */
    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) return;
        try {
            JwtService.ParsedClaims claims = jwtService.validateRefreshToken(refreshTokenValue);
            UUID jti = UUID.fromString(claims.jti());
            refreshTokenRepo.findByJtiForUpdate(jti).ifPresent(record -> {
                if (!record.revoked()) {
                    refreshTokenRepo.revokeByJti(jti);
                }
            });
        } catch (TokenValidationException ignored) {
            // Expired or invalid token — cookies are cleared regardless.
        }
    }

    /**
     * Returns the current user's profile.
     *
     * @throws ResourceNotFoundException if the account no longer exists or is inactive
     */
    public AuthResponse me(Long userId) {
        return userRepo.findById(userId)
                .filter(UserAccount::active)
                .map(AuthService::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private static AuthResponse toResponse(UserAccount u) {
        return new AuthResponse(u.id(), u.email(), u.displayName(), u.role());
    }
}
