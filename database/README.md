# TaskFlow — Database

PostgreSQL database assets for the TaskFlow application. This component owns all SQL migration files, stored procedures, functions, views, seed scripts, and database-level documentation.

---

## Purpose

This directory is the single source of truth for the TaskFlow database schema and SQL objects. The Spring Boot backend applies these assets at startup via Flyway; no SQL schema should be defined or evolved outside this directory.

---

## Folder Structure

```text
database/
├── migrations/     # Versioned Flyway SQL migration files (schema evolution)
├── procedures/     # PL/pgSQL stored procedure definitions
├── functions/      # PL/pgSQL function definitions
├── views/          # SQL view definitions
├── seed/           # Reference and development seed data scripts
├── tests/          # Database-level integration and unit tests
├── performance/    # Benchmarks, EXPLAIN plans, and index analysis queries
├── scripts/        # Utility scripts (setup helpers, local reset, etc.)
├── docs/           # Architecture notes and SQL conventions
├── .env.example    # Template for local database connection settings
└── .gitignore      # Excludes .env and other local secrets
```

---

## SQL Migration Naming Convention

Migration files live in `migrations/` and follow the Flyway versioned naming scheme:

```
V<version>__<description>.sql
```

Examples:

```
V1__initial_schema.sql
V2__create_indexes.sql
V3__create_example_procedure.sql
```

Rules:

- The version prefix (`V1`, `V2`, …) determines execution order.
- The double underscore (`__`) separates the version from the description.
- Descriptions use lowercase words joined by underscores.
- Migration files are **immutable** once applied to a shared environment (staging, production). Never edit an applied migration; add a new one instead.
- Schema changes must be reviewed and tested before being merged.

---

## SQL Object Locations

| Object type        | Directory       |
|--------------------|-----------------|
| Schema migrations  | `migrations/`   |
| Stored procedures  | `procedures/`   |
| Functions          | `functions/`    |
| Views              | `views/`        |
| Seed / fixture data| `seed/`         |
| Tests              | `tests/`        |
| Performance queries| `performance/`  |
| Utility scripts    | `scripts/`      |

All stored procedures, functions, and views are introduced via versioned migration files in `migrations/`. The files in `procedures/`, `functions/`, and `views/` serve as the canonical readable reference for those objects but are not executed independently.

---

## Relationship to the Spring Boot Backend

- The backend (`backend/`) depends on this directory for its schema.
- Flyway is configured inside the Spring Boot application and runs automatically on startup.
- The backend consumes database objects defined here but does not own SQL schema definitions.
- Flyway configuration (`spring.flyway.*` properties) and the classpath migration path will be introduced in a later phase.

---

## Prerequisites

PostgreSQL must be available and reachable before any database integration tests or Flyway migrations can run. This setup does **not** install PostgreSQL; use an existing local installation or a Docker-based environment.

---

## Connecting to a Local Database

The following commands assume a local PostgreSQL instance is running and a database has been created. Adjust host, port, user, and database name as needed.

```bash
# Interactive psql session
psql -h localhost -p 5432 -U taskflow_app -d taskflow

# Run a specific SQL file
psql -h localhost -p 5432 -U taskflow_app -d taskflow -f migrations/V1__initial_schema.sql

# List all databases
psql -h localhost -p 5432 -U taskflow_app -l
```

Copy `.env.example` to `.env` and fill in your local credentials before running scripts that read environment variables.

---

## Future Work

- Actual Flyway migrations will be added in a subsequent implementation phase.
- Docker Compose integration for local database provisioning will be added separately.
- CI/CD database testing pipeline will be configured when integration tests are introduced.
