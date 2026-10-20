# functions/

Canonical reference files for PL/pgSQL and SQL functions.

## Conventions

- Use `CREATE OR REPLACE FUNCTION` for definitions.
- Always declare the return type explicitly.
- Prefer `RETURNS TABLE` or composite types over `RETURNS SETOF RECORD` for readability.
- Mark functions `IMMUTABLE`, `STABLE`, or `VOLATILE` accurately — the planner uses this information.
- Use `SECURITY DEFINER` only when explicitly required; document the reason when used.
- Schema-qualify references and use parameterized logic (no dynamic SQL from untrusted input).
- Introduce functions through versioned migration files in `migrations/`.

No function files exist yet.
