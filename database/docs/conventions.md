# SQL Conventions

This document defines the SQL standards for the TaskFlow database. All contributors must follow these conventions to keep the codebase consistent, readable, and safe.

---

## General Style

- Use lowercase for SQL keywords (`select`, `insert`, `create table`, etc.). Uppercase is acceptable in comments and documentation but should be consistent within a file.
- Use 4-space indentation.
- End every statement with a semicolon.
- Add a brief comment above any non-obvious statement.

---

## Naming

| Object           | Convention                          | Example                    |
|------------------|-------------------------------------|----------------------------|
| Tables           | `snake_case`, plural noun           | `tasks`, `project_members` |
| Columns          | `snake_case`                        | `created_at`, `user_id`    |
| Primary keys     | `id`                                | `id`                       |
| Foreign keys     | `<referenced_table_singular>_id`    | `project_id`, `user_id`    |
| Indexes          | `idx_<table>_<columns>`             | `idx_tasks_project_id`     |
| Procedures       | `<verb>_<object>` (snake_case)      | `archive_completed_tasks`  |
| Functions        | `<verb>_<object>` or `get_<value>`  | `get_overdue_task_count`   |
| Views            | `v_<description>`                   | `v_active_tasks`           |
| Sequences        | `<table>_<column>_seq`              | `tasks_id_seq`             |

---

## Migrations

- One focused change per migration file.
- Migration files are immutable after being applied to a shared environment.
- Use `IF NOT EXISTS` / `IF EXISTS` guards where appropriate to make migrations re-runnable in development.
- Test each migration against a clean database locally before committing.

---

## Stored Procedures

```sql
-- Header comment: purpose, parameters, transaction behavior, side effects.
CREATE OR REPLACE PROCEDURE public.procedure_name(
    p_param_one integer,
    p_param_two text
)
LANGUAGE plpgsql
AS $$
BEGIN
    -- Body
    -- Use schema-qualified names: public.table_name
    -- Use parameterized queries; never build SQL strings from untrusted input.
    -- Handle exceptions deliberately:
    --   EXCEPTION WHEN unique_violation THEN ...
    -- Do not silently swallow errors with WHEN OTHERS THEN NULL.
END;
$$;
```

- Document `COMMIT` / `ROLLBACK` points in the header comment.
- Avoid duplicating application-layer business logic unless there is a clear requirement (e.g., atomicity, performance).
- Introduce each procedure through a versioned migration file.

---

## Functions

```sql
CREATE OR REPLACE FUNCTION public.function_name(
    p_input integer
)
RETURNS integer
LANGUAGE plpgsql
STABLE  -- or IMMUTABLE / VOLATILE as appropriate
AS $$
DECLARE
    v_result integer;
BEGIN
    -- Body
    RETURN v_result;
END;
$$;
```

- Mark volatility accurately (`IMMUTABLE`, `STABLE`, `VOLATILE`). The query planner depends on this.
- Avoid `SECURITY DEFINER` unless explicitly required; document the reason when used.

---

## Views

```sql
CREATE OR REPLACE VIEW public.v_view_name AS
select
    t.id,
    t.title,
    p.name as project_name
from public.tasks t
join public.projects p on p.id = t.project_id
where t.deleted_at is null;
```

---

## Security

- Use parameterized queries in dynamic SQL (via `EXECUTE ... USING`). Never concatenate untrusted input into a SQL string.
- Do not store plaintext passwords or secrets in the database.
- Use application roles with least-privilege access. Do not use the superuser role for application connections.
- Never commit real credentials. Use `.env` (excluded from Git) for local connection settings.

---

## Data Types

| Use case              | Preferred type          |
|-----------------------|-------------------------|
| Primary key           | `bigserial` or `uuid`   |
| Timestamps            | `timestamptz`           |
| Short text            | `varchar(n)` or `text`  |
| Money / currency      | `numeric(19,4)`         |
| Booleans              | `boolean`               |
| JSON payloads         | `jsonb`                 |
