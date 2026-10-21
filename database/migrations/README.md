# migrations/

Versioned SQL migration files executed by Flyway in ascending version order.

## Naming Convention

```
V<version>__<description>.sql
```

- `<version>` — integer, unique, determines execution order (e.g. `1`, `2`, `10`)
- `<description>` — lowercase words separated by underscores

## Rules

- Files are **immutable** once applied to a shared environment. To correct a mistake, add a new migration.
- Each migration should be a single, focused change (one table, one index set, one procedure, etc.).
- Include comments inside the file explaining the intent of non-obvious changes.
- Test migrations locally against a clean database before committing.

## Applied migrations

| File | Description | Phase |
|------|-------------|-------|
| `V1__initial_schema.sql` | Creates `users`, `projects`, `project_members`, `tasks` tables with all constraints and indexes | T4.2 |

## Pending migrations

None at this time. New migrations will be added as features are implemented.

## Example future naming

```
V2__add_password_hash_to_users.sql   -- Authentication phase
V3__create_comments_table.sql        -- Comments phase
V4__add_task_ordering.sql            -- Task management phase
```

# 1. Confirm the container is healthy
docker compose ps

# 2. Copy the migration file into the container
docker cp database/migrations/V1__initial_schema.sql taskflow-postgres:/tmp/V1__initial_schema.sql

# 3. Apply the migration
docker exec taskflow-postgres psql -U postgres -d TaskFlow -f /tmp/V1__initial_schema.sql

# 4. Verify the four tables exist
docker exec taskflow-postgres psql -U postgres -d TaskFlow -c "\dt public.*"

# 5. Verify all 14 constraints exist
docker exec taskflow-postgres psql -U postgres -d TaskFlow -c "
select conrelid::regclass as table_name, conname, contype
from pg_constraint
where conrelid::regclass::text in ('users','projects','project_members','tasks')
order by table_name, contype, conname;"

# 6. Verify the three indexes exist
docker exec taskflow-postgres psql -U postgres -d TaskFlow -c "
select indexname, tablename
from pg_indexes
where tablename in ('users','projects','project_members','tasks')
  and schemaname = 'public'
order by tablename, indexname;"

# 7. Clean up the temp file from the container
docker exec taskflow-postgres rm /tmp/V1__initial_schema.sql

