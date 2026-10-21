# scripts/

Utility shell and SQL scripts for local development workflows.

## Rules

- Scripts that drop or reset data must include a guard preventing execution outside of local environments.
- Do not use these scripts as a substitute for proper Flyway-managed migrations.
- Do not commit scripts containing real credentials.

## Scripts

| File | Purpose | When to run |
|------|---------|-------------|
| `setup_roles.sql` | Creates the `taskflow_app` database role with least-privilege DML grants | Once per environment, before first application start |

### Running setup_roles.sql locally

```bash
# From repo root — replace 'change_me_locally' with your local password first
docker exec -i taskflow-postgres psql -U postgres -d TaskFlow < database/scripts/setup_roles.sql
```

After running, update `backend/.env` so `SPRING_DATASOURCE_USERNAME=taskflow_app` and `SPRING_DATASOURCE_PASSWORD=<your-password>` to connect as the restricted role instead of the superuser.
