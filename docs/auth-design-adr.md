# ADR: JWT + HttpOnly Cookie Authentication

**Status:** Accepted  
**Date:** 2026-10-21  
**Components:** Backend, Frontend, Database  
**Implemented in:** T9.2–T9.7

---

## Decision

Use short-lived JWT access tokens and long-lived JWT refresh tokens, both stored in `HttpOnly` cookies set by the backend. The frontend JavaScript never reads token values; it sends cookies automatically via `credentials: "include"`.

---

## Token design

### Access token

| Property | Value |
|----------|-------|
| Lifetime | 15 minutes (env: `ACCESS_TOKEN_TTL_MINUTES`, default `15`) |
| Algorithm | HS256 |
| Cookie name | `tf_access` |
| Cookie path | `/api` |

### Refresh token

| Property | Value |
|----------|-------|
| Lifetime | 7 days (env: `REFRESH_TOKEN_TTL_DAYS`, default `7`) |
| Algorithm | HS256 |
| Cookie name | `tf_refresh` |
| Cookie path | `/api/v1/auth/refresh` |

The narrow `Path` on the refresh cookie means the browser only sends it to the refresh endpoint, not to every API call — this limits its exposure.

### JWT claims

Both token types carry only these claims (no passwords, emails, or sensitive personal data):

| Claim | Type | Description |
|-------|------|-------------|
| `sub` | `String` | User ID |
| `type` | `String` | `"access"` or `"refresh"` |
| `iat` | `long` | Issued-at (epoch seconds) |
| `exp` | `long` | Expiry (epoch seconds) |
| `iss` | `String` | `"taskflow"` |
| `jti` | `String` | Unique token ID (UUID v4) |

The `type` claim is validated on every request — a refresh token is rejected if presented to a protected endpoint, and an access token is rejected at the refresh endpoint.

---

## Cookie attributes

### Production (HTTPS)

```
Set-Cookie: tf_access=<jwt>; HttpOnly; Secure; SameSite=Strict; Path=/api; Max-Age=900
Set-Cookie: tf_refresh=<jwt>; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth/refresh; Max-Age=604800
```

### Local development (HTTP)

`Secure` is omitted. All other attributes are identical. A single `COOKIE_SECURE` env var controls this (default `true`; set `false` in local `.env`).

---

## Signing key

- Algorithm: HS256 with a secret of at least 256 bits (32 bytes).
- Env var: `JWT_SECRET` — never committed; required at startup.
- The same key signs both access and refresh tokens; the `type` claim (not the key) distinguishes them.
- Key rotation: not automated in this phase; requires a rolling deployment that accepts both keys during transition.

---

## CORS

CORS is already configured (`CorsConfig.java`):
- Allowed origins: explicit list via env var `CORS_ALLOWED_ORIGINS` — never a wildcard.
- `allowCredentials(true)` is set, which requires a non-wildcard origin.
- Allowed methods: `GET, POST, PATCH, PUT, DELETE, OPTIONS`.
- This configuration must be kept in sync with any new frontend origin added in later environments.

---

## CSRF protection

`SameSite=Strict` is the primary CSRF mitigation: browsers will not send these cookies on cross-site requests.

For defense-in-depth, the backend also validates the `Origin` header on all state-changing methods (`POST`, `PATCH`, `PUT`, `DELETE`). The value must match one of the configured `CORS_ALLOWED_ORIGINS`. `Referer`-only fallback is accepted when `Origin` is absent (some browsers omit it on same-origin navigation).

A separate CSRF token is not needed because `SameSite=Strict` + `Origin` validation covers all targeted attack vectors without requiring per-form tokens.

---

## Refresh token rotation and reuse detection

1. **On successful refresh:** the old refresh token is immediately invalidated in the database; a new access token and new refresh token are issued.
2. **Reuse detection:** each refresh token belongs to a *token family* (UUID stored alongside its `jti` in the database). If a revoked token is replayed, the entire family is invalidated — all sessions for that user that share the family are logged out.
3. **Concurrency:** a database-level unique constraint on `jti` ensures that concurrent refresh requests using the same token only one wins; the second receives a 401 and triggers family revocation.
4. **Storage:** the database stores `(jti, family_id, user_id, revoked, expires_at)` for refresh tokens. Access token `jti`s are not stored — their short lifetime makes server-side revocation unnecessary.

---

## Logout and revocation

- **Logout:** the backend marks the refresh token `jti` as revoked in the database and responds with two `Set-Cookie` headers that clear both cookies:
  ```
  Set-Cookie: tf_access=; HttpOnly; Secure; SameSite=Strict; Path=/api; Max-Age=0
  Set-Cookie: tf_refresh=; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth/refresh; Max-Age=0
  ```
- **Access token revocation:** not supported in this phase; 15-minute lifetime limits the blast radius of a stolen access token.
- **Expired refresh token:** responds `401 REFRESH_TOKEN_EXPIRED`. The frontend redirects the user to the login page.

---

## Frontend contract

| Rule | Rationale |
|------|-----------|
| All `fetch`/RTK Query calls use `credentials: "include"` | Sends cookies cross-origin (already set in `store/api/base.ts`) |
| No token value is read from `document.cookie`, `localStorage`, or `sessionStorage` | `HttpOnly` enforces this for cookies; no JS token storage allowed |
| The login page redirects to `/login` on any `401` response | Handled centrally in a response middleware, not per-endpoint |
| The refresh call is made to `POST /api/v1/auth/refresh` | Only the browser sends `tf_refresh` to this path |

---

## Security risks and mitigations summary

| Risk | Mitigation |
|------|------------|
| XSS reads token | `HttpOnly` prevents JS cookie access |
| CSRF forges state-changing request | `SameSite=Strict` + `Origin` validation |
| Refresh token replay / theft | Rotation + reuse detection + narrow cookie path |
| Signing key compromise | Strong secret via env var, not committed; rotation plan above |
| Access token theft (network) | `Secure` in prod enforces HTTPS-only transmission |
| Concurrent refresh race | DB unique constraint on `jti`; loser triggers revocation |
| Long-lived session abuse | 7-day refresh TTL; family revocation on reuse detection |

---

## Database additions (implemented in T9.3)

A new migration will add:

```sql
CREATE TABLE refresh_tokens (
    jti         UUID        PRIMARY KEY,
    family_id   UUID        NOT NULL,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens(family_id);
```

---

## Environment variables introduced by this design

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | *(required)* | HS256 signing secret, min 32 bytes |
| `ACCESS_TOKEN_TTL_MINUTES` | `15` | Access token lifetime in minutes |
| `REFRESH_TOKEN_TTL_DAYS` | `7` | Refresh token lifetime in days |
| `COOKIE_SECURE` | `true` | Set `false` for local HTTP development |
