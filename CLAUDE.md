# TaskFlow — Monorepo Claude Code Guide

This is the root-level guide for the TaskFlow monorepo. Read it together with the component-level guides before making any change.

## Monorepo layout

```
TaskFlow/
├── frontend/    # Next.js — see frontend/CLAUDE.md and frontend/.claude/
├── backend/     # Spring Boot — see backend/CLAUDE.md and backend/.claude/
├── database/    # PostgreSQL assets — see database/CLAUDE.md and database/.claude/
```

Each component has its own `CLAUDE.md` and `.claude/rules/` directory. Read them before working on that component.

## Read order

1. This file (root context and cross-component rules)
2. The relevant component's `CLAUDE.md`
3. The component's `.claude/rules/` files
4. Any referenced skill or command files

## Cross-component rules

- The frontend calls the backend REST API over HTTP. It never connects directly to PostgreSQL.
- The `database/` component owns SQL migration files. The backend invokes Flyway; it does not define schema in Java code.
- Business rules live in the backend. Do not duplicate them in stored procedures without a documented reason.
- All REST endpoints are versioned under `/api/v1`.
- Secrets are never committed. Each component has a `.env.example` with placeholder values only.
- One Git repository at the repository root. Do not run `git init` inside component directories.

## API contract

The agreed JSON error response shape and local URLs are defined in `README.md`. Update that document when API conventions change.

## Shared conventions

- **Local frontend URL:** `http://localhost:3000`
- **Local backend URL:** `http://localhost:8080`
- **API prefix:** `/api/v1`
- **Error response:** see `README.md` — stable `code`, `message`, `details`, `timestamp`, `path` fields
- **Migration owner:** `database/migrations/`; loaded by the Spring Boot backend via Flyway

## Before any cross-component change

1. Read the impacted components' CLAUDE.md and rules.
2. Confirm the change does not break the API contract or migration ownership rules.
3. Update `README.md` if URLs, error formats, or environment variables change.
4. Update relevant component READMEs if commands or setup steps change.
