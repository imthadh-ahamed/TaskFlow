package com.taskflow.auth.application;

import com.taskflow.auth.api.AuthResponse;
import com.taskflow.auth.api.LoginRequest;
import com.taskflow.auth.api.RegisterRequest;
import com.taskflow.auth.domain.UserAccount;
import com.taskflow.auth.infrastructure.RefreshTokenRepository;
import com.taskflow.auth.infrastructure.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

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

    private static AuthResponse toResponse(UserAccount u) {
        return new AuthResponse(u.id(), u.email(), u.displayName(), u.role());
    }
}
