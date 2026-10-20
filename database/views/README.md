# views/

Canonical reference files for SQL views.

## Conventions

- Use `CREATE OR REPLACE VIEW` for view definitions.
- Prefer views for commonly joined or filtered query shapes that are read-only.
- Use `CREATE MATERIALIZED VIEW` only when query performance requires it, and document the refresh strategy.
- Schema-qualify all object references.
- Introduce views through versioned migration files in `migrations/`.

No view files exist yet.
