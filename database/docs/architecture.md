# Database Architecture

## Overview

TaskFlow uses a single PostgreSQL database. The schema is managed exclusively through Flyway versioned migrations stored in `migrations/`. The Spring Boot backend is responsible for running Flyway on startup.

## Component Relationships

```
┌─────────────────────┐        ┌──────────────────────────┐
│  Spring Boot Backend │──────▶│  PostgreSQL (taskflow DB) │
│  (Flyway client)     │        │                          │
└─────────────────────┘        │  - Tables & constraints  │
                                │  - Indexes               │
                                │  - Stored procedures     │
                                │  - Functions             │
                                │  - Views                 │
                                └──────────────────────────┘
```

## Schema Management Principles

- The `database/` directory is the single source of truth for all SQL schema and objects.
- No SQL DDL should be written inside the backend application code (e.g., no JPA `hbm2ddl.auto=create`).
- Schema evolution happens only through new, immutable migration files.
- The backend's Flyway configuration will point to `classpath:db/migration`, which is populated from this directory during the build.

## Environments

| Environment | Migration strategy                        |
|-------------|-------------------------------------------|
| Local dev   | Flyway runs on Spring Boot startup        |
| Staging     | Flyway runs on Spring Boot startup        |
| Production  | Flyway runs on Spring Boot startup (gated)|

## Future Decisions (not yet made)

- Connection pooling strategy (PgBouncer vs. HikariCP only)
- Read replica configuration
- Backup and point-in-time recovery policy
- Schema ownership and role separation
