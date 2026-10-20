---
description: Validate database file structure and available SQL migration conventions without making changes
---

Inspect the database directory and validate the files relevant to the current task.

- Check migration naming and version ordering.
- Check references to SQL objects and migration dependencies.
- If a PostgreSQL test instance is available and execution is explicitly authorized, use a disposable test database for syntax/migration validation.
- Do not connect to shared or production databases without explicit authorization.
- Do not run destructive statements.
- Report the exact checks performed and distinguish static inspection from actual database execution.
