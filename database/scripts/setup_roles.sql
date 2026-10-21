-- setup_roles.sql
-- Creates the least-privilege application role used by the Spring Boot backend.
-- Run ONCE as a superuser before first deployment. This script is idempotent.
--
-- Usage (Docker local):
--   docker exec -i taskflow-postgres psql -U postgres -d TaskFlow < database/scripts/setup_roles.sql
--
-- IMPORTANT: Replace 'change_me_locally' with a real password before running.
-- Never commit a version of this file containing a real password.

-- Create the application role if it does not already exist.
do $$
begin
    if not exists (select 1 from pg_roles where rolname = 'taskflow_app') then
        create role taskflow_app with login password 'change_me_locally';
    end if;
end
$$;

-- Allow the role to use the public schema.
grant usage on schema public to taskflow_app;

-- Grant DML on tables that already exist.
grant select, insert, update, delete
    on all tables in schema public
    to taskflow_app;

-- Grant DML on tables created by future migrations.
alter default privileges in schema public
    grant select, insert, update, delete on tables to taskflow_app;

-- Grant sequence usage (required for bigserial INSERT).
grant usage, select
    on all sequences in schema public
    to taskflow_app;

alter default privileges in schema public
    grant usage, select on sequences to taskflow_app;

-- The taskflow_app role has no CREATE / DROP / ALTER privileges.
-- Flyway migrations must run as a superuser or a role with schema-alter rights
-- (e.g. the 'postgres' user used during local development and CI migration steps).
