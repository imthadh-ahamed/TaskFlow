-- V4__add_refresh_tokens.sql
-- Creates the refresh_tokens table for JWT refresh-token rotation and
-- reuse detection. Design decisions documented in docs/auth-design-adr.md.
--
-- Key design choices:
--
--   jti (UUID) — the JWT "jti" claim from the signed refresh token.
--     This is a token *identifier*, not the raw token. The raw JWT cannot be
--     reconstructed from the jti alone; it requires the signing key.
--     Stored as UUID for compact indexing (vs. storing a full JWT string or its hash).
--
--   family_id (UUID) — groups all tokens issued from the same login session.
--     On successful rotation: old row set revoked=true, new row inserted with
--     the same family_id.
--     On reuse detection (revoked token presented): all rows in the family are
--     set revoked=true, immediately terminating all sessions in that family.
--
--   replaced_by_jti — nullable self-reference to the successor token after
--     rotation. Null while the token is active (not yet rotated). Allows
--     auditing the rotation chain and confirming that a token was legitimately
--     replaced rather than just revoked.
--
--   revoked — explicit boolean rather than a deleted row so that revocation
--     events and the reuse-detection audit trail are preserved.
--
--   expires_at — stored so that expired tokens can be purged by a background
--     job without parsing the JWT. Tokens past this time are always rejected
--     regardless of the revoked flag.
--
-- Access tokens are NOT stored here. Their 15-minute lifetime makes server-side
-- revocation unnecessary for this phase (see ADR).

create table public.refresh_tokens (
    jti             uuid         primary key,
    family_id       uuid         not null,
    user_id         bigint       not null,
    revoked         boolean      not null default false,
    replaced_by_jti uuid,
    expires_at      timestamptz  not null,
    created_at      timestamptz  not null default now(),

    constraint refresh_tokens_user_fk
        foreign key (user_id) references public.users (id)
        on delete cascade,

    -- Self-referential FK: replaced_by_jti must be a known token.
    -- SET NULL on delete so that purging an old token does not break the chain.
    constraint refresh_tokens_replaced_by_fk
        foreign key (replaced_by_jti) references public.refresh_tokens (jti)
        on delete set null
);

-- ---------------------------------------------------------------------------
-- Indexes
-- ---------------------------------------------------------------------------

-- Session management: list or revoke all sessions for a user
create index idx_refresh_tokens_user_id
    on public.refresh_tokens (user_id);

-- Reuse-detection response: revoke every token in the same family
create index idx_refresh_tokens_family_id
    on public.refresh_tokens (family_id);

-- Cleanup: find expired tokens for a periodic purge job.
-- Partial index excludes already-revoked tokens (already cleaned up or
-- in line to be cleaned up by a different path).
create index idx_refresh_tokens_expires_at
    on public.refresh_tokens (expires_at)
    where revoked = false;
