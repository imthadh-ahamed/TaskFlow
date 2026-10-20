# scripts/

Utility shell and SQL scripts for local development workflows.

## Planned Contents

- `create_local_db.sh` — create and configure a local development database
- `reset_local_db.sh` — drop and recreate the local database (local only, never production)
- `run_migrations.sh` — wrapper to invoke Flyway migrations manually

## Rules

- Scripts that drop or reset data must include a guard preventing execution outside of local environments.
- Do not use these scripts as a substitute for proper Flyway-managed migrations.
- Do not commit scripts containing real credentials.

No utility scripts exist yet.
