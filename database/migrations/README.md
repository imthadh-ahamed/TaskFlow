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

## Example

```
V1__initial_schema.sql          -- Creates core tables
V2__create_indexes.sql          -- Adds indexes for common query patterns
V3__create_example_procedure.sql -- Introduces a utility stored procedure
```

No migration files exist yet. They will be added during the feature implementation phase.
