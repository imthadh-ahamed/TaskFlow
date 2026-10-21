package com.taskflow.auth.application;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Signs and validates JWTs using HS256.
 * The secret must be at least 32 characters (256 bits) — JJWT enforces this at key-creation time.
 * Password hashes, emails, and other sensitive data are never put in claims.
 */
@Service
public class JwtService {

    private static final String ISSUER = "taskflow";
    private static final String CLAIM_TYPE = "type";
    static final String TYPE_ACCESS = "access";
    static final String TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final long accessTtlMs;
    private final long refreshTtlMs;

    JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-ttl-minutes:15}") long accessTtlMinutes,
            @Value("${jwt.refresh-token-ttl-days:7}") long refreshTtlDays
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMs = accessTtlMinutes * 60_000L;
        this.refreshTtlMs = refreshTtlDays * 86_400_000L;
    }

    /**
     * A pair of freshly signed access + refresh tokens for the same login session.
     * Both tokens share one {@code familyId} for refresh-token rotation tracking.
     * {@code refreshExpiry} is pre-computed so the caller can store it without re-parsing the JWT.
     */
    public record TokenPair(
            String accessToken,
            UUID accessJti,
            String refreshToken,
            UUID refreshJti,
            UUID familyId,
            Instant refreshExpiry
    ) {}

    /** Claims extracted from a validated token. */
    public record ParsedClaims(Long userId, String jti) {}

    /** Generates a linked access + refresh token pair for the given user. */
    public TokenPair generateTokenPair(Long userId) {
        UUID familyId = UUID.randomUUID();
        UUID accessJti = UUID.randomUUID();
        UUID refreshJti = UUID.randomUUID();
        Instant refreshExpiry = Instant.now().plus(refreshTtlMs, ChronoUnit.MILLIS);
        return new TokenPair(
                buildToken(userId, TYPE_ACCESS, accessJti, accessTtlMs),
                accessJti,
                buildToken(userId, TYPE_REFRESH, refreshJti, refreshTtlMs),
                refreshJti,
                familyId,
                refreshExpiry
        );
    }

    /**
     * Validates a token string expected to be an access token.
     * Throws {@link TokenValidationException} if the token is expired, tampered, or the wrong type.
     */
    public ParsedClaims validateAccessToken(String token) {
        return parseAndValidate(token, TYPE_ACCESS);
    }

    /**
     * Validates a token string expected to be a refresh token.
     * Throws {@link TokenValidationException} if the token is expired, tampered, or the wrong type.
     */
    public ParsedClaims validateRefreshToken(String token) {
        return parseAndValidate(token, TYPE_REFRESH);
    }

    // -------------------------------------------------------------------------

    private String buildToken(Long userId, String type, UUID jti, long ttlMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .issuer(ISSUER)
                .id(jti.toString())
                .signWith(signingKey)
                .compact();
    }

    private ParsedClaims parseAndValidate(String token, String expectedType) {
        io.jsonwebtoken.Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(ISSUER)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new TokenValidationException("Token has expired");
        } catch (JwtException e) {
            throw new TokenValidationException("Invalid token");
        }

        String type = claims.get(CLAIM_TYPE, String.class);
        if (!expectedType.equals(type)) {
            throw new TokenValidationException("Wrong token type");
        }

        return new ParsedClaims(Long.parseLong(claims.getSubject()), claims.getId());
    }
}
