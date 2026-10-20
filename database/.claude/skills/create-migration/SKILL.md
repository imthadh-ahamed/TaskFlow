---
name: create-migration
description: Create a safe, reviewable PostgreSQL Flyway migration when explicitly requested.
---

# Create a PostgreSQL migration

1. Inspect the database directory and existing migration history.
2. Determine the next version without reusing an existing version.
3. Confirm the requested schema change and dependencies.
4. Create a Flyway-compatible file such as `V4__add_task_priority.sql`.
5. Use explicit constraints and naming, and consider locks, data volume, and compatibility.
6. Do not rewrite migrations that may already have been applied to shared environments.
7. Do not run the migration against a shared or production database without explicit authorization.
8. If local PostgreSQL is available and execution is authorized, test in a disposable database.
9. Document the change and report validation results honestly.
