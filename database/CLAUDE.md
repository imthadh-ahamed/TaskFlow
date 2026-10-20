# TaskFlow Database — Claude Code Guide

## Project context
This is the database component of the TaskFlow monorepo:
- `frontend/`: Next.js + React + TypeScript
- `backend/`: Java + Spring Boot
- `database/`: PostgreSQL schema, migrations, procedures, functions, views, seed data, and database documentation

Work only within the `database/` directory unless the user explicitly requests a monorepo-wide change. Do not modify frontend, backend, or root-level files during database-only tasks.

## Scope and current baseline
This directory is intended for PostgreSQL SQL assets and documentation. Do not assume PostgreSQL is installed, running, or connected. Do not assume a schema, migration history, or production database exists. Inspect the existing files before editing.

The initial setup is documentation and structure only. Do not create application tables, indexes, triggers, functions, stored procedures, or views unless explicitly requested.

## Working principles
1. Inspect existing SQL assets and documentation before making changes.
2. Make the smallest complete change that satisfies the request.
3. Keep SQL changes explicit, reviewable, and version-controlled.
4. Use PostgreSQL SQL and PL/pgSQL; do not introduce another database dialect.
5. Keep schema changes and SQL assets in this component; the Spring Boot backend owns application logic and invokes Flyway when configured.
6. Avoid duplicating business rules in stored procedures and backend code without a documented reason.
7. Never include real credentials, tokens, or production connection strings in source control.
8. Do not connect to production or shared databases unless the user explicitly authorizes the target and operation.
9. Never run destructive SQL, data deletion, schema drops, or irreversible migrations without explicit approval.
10. Do not add infrastructure or dependencies merely for future possibilities.

## Migration conventions
- Use Flyway-compatible versioned migration names, such as `V1__initial_schema.sql`.
- Use double underscores between the version and description.
- Treat migrations applied to shared environments as immutable; add a new migration to change the schema.
- Review migrations for constraints, indexes, locking, data volume, and rollback/recovery implications.
- Do not assume Flyway is configured in the backend until its build and configuration confirm it.
- Keep seed data separate from schema migrations unless the project explicitly adopts a different convention.
- Do not create a migration just to demonstrate syntax unless asked.

## Stored procedures and functions
- Use PostgreSQL `CREATE OR REPLACE PROCEDURE` where appropriate for procedures and `CREATE OR REPLACE FUNCTION` for functions.
- Choose procedures and functions based on their intended behavior and PostgreSQL semantics.
- Use explicit parameter names and types, and schema-qualify object names where it improves clarity and safety.
- Use PL/pgSQL only when procedural control flow is needed; prefer straightforward SQL for simple operations.
- Handle errors deliberately. Do not catch and silently suppress exceptions.
- Avoid unsafe dynamic SQL. If dynamic identifiers are required, use PostgreSQL-safe identifier quoting and validate inputs; bind values where possible.
- Document side effects, transaction assumptions, security context, and expected caller permissions.
- Be mindful of `SECURITY DEFINER`; avoid it unless necessary and configure a safe `search_path`.
- Do not duplicate backend business logic in database routines without a clear, documented requirement.

## SQL quality and safety
- Use explicit column lists rather than `SELECT *` in durable scripts and procedures.
- Use parameterized operations for values; never concatenate untrusted values into SQL.
- Use constraints to enforce data integrity where appropriate.
- Use schema-qualified names where ambiguity or search-path risk exists.
- Avoid speculative indexes; justify indexes against expected query patterns and validate with query plans when possible.
- Avoid `DROP`, `TRUNCATE`, or broad `DELETE` operations unless specifically authorized and carefully scoped.
- Consider lock duration and table size for production schema changes.
- Do not assume a migration can be rolled back automatically; document recovery steps when relevant.

## Testing and validation
- Inspect available SQL assets and scripts before selecting validation steps.
- Do not claim SQL was executed unless it was actually run against a PostgreSQL instance.
- If PostgreSQL is available and the user authorizes local validation, use a disposable local/test database.
- For migrations, validate ordering, naming, syntax, dependencies, and repeatability assumptions.
- For procedures, test normal cases, boundary cases, permissions, errors, and transaction behavior where relevant.
- Report unavailable validation honestly if no PostgreSQL server or test environment is available.

## Safe tool use
- Do not install system-wide software, create cloud resources, or change database credentials without explicit permission.
- Do not execute migrations against shared or production databases without explicit confirmation.
- Do not reset Git history, remove broad directories, or overwrite existing user files without permission.
- Review any script that can modify or delete data before running it.

## Completion response
Summarize:
1. Files created or changed
2. Important schema/procedure design decisions
3. Commands or database tests actually run and their results
4. Risks, assumptions, and follow-up work
