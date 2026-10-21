-- V3__add_auth_fields_to_users.sql
-- Adds authentication fields to the users table for the JWT auth phase (T9.x).
-- Design decisions documented in docs/auth-design-adr.md and
-- database/docs/schema-design.md (updated in T9.2).
--
-- Changes:
--   password_hash — BCrypt-hashed password; raw password is never stored.
--   role          — User authorization role; defaults to 'user' on insert.
--
-- Migration safety:
--   Both columns are added with defaults so the statement does not block on
--   a table scan of existing rows. The default on password_hash is then
--   dropped — the application must always supply it on INSERT.
--   The default on role is kept because 'user' is the expected value for
--   every new registration.

alter table public.users
    add column password_hash text not null default '',
    add column role          text not null default 'user';

-- Constrain role to the supported values.
alter table public.users
    add constraint users_role_values
        check (role in ('user', 'admin'));

-- Remove the migration-only default from password_hash.
-- Every INSERT must provide a real BCrypt hash; an empty string is not valid.
alter table public.users
    alter column password_hash drop default;

-- Sparse index for admin lookups; regular users are the overwhelming majority
-- so a partial index keeps the index small.
create index idx_users_role_admin
    on public.users (id)
    where role = 'admin';
