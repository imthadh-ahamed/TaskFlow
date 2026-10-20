# procedures/

Canonical reference files for PL/pgSQL stored procedures. These files document the current intended definition of each procedure. Procedures are introduced and changed exclusively through versioned migration files in `migrations/`.

## Conventions

See `docs/conventions.md` for the full SQL conventions reference. Key rules for procedures:

- Use `CREATE OR REPLACE PROCEDURE` for definitions.
- Use PL/pgSQL (`LANGUAGE plpgsql`) for any procedural logic.
- Use explicit, descriptive parameter names and fully qualified types.
- Schema-qualify object references where ambiguity is possible (e.g. `public.table_name`).
- Handle exceptions deliberately — do not use bare `EXCEPTION WHEN OTHERS THEN NULL`.
- Document transaction behavior (`COMMIT`/`ROLLBACK`) and any side effects in a header comment.
- Use parameterized queries; never concatenate untrusted input into dynamic SQL strings.
- Keep business logic in procedures only when there is a clear, documented requirement. Prefer the application layer for complex logic.

## File Naming

```
<schema>_<object_name>.sql
```

Example: `public_archive_completed_tasks.sql`

No procedure files exist yet. They will be added alongside their corresponding migration files.
