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
