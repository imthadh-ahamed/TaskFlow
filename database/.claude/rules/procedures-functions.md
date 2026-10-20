# Stored procedure and function rules

- Use `CREATE OR REPLACE PROCEDURE` for procedures and `CREATE OR REPLACE FUNCTION` for functions where appropriate.
- Choose procedure vs function based on PostgreSQL semantics and how the object will be called.
- Use explicit parameter names and types.
- Use PL/pgSQL only when procedural control flow is needed.
- Document purpose, inputs, outputs, side effects, permissions, and transaction assumptions.
- Do not swallow exceptions; handle only errors that can be meaningfully addressed.
- Avoid unsafe dynamic SQL. Quote dynamic identifiers safely and bind values wherever possible.
- Avoid `SECURITY DEFINER` unless required; if used, set a safe `search_path` and document permissions.
- Keep business rules in the backend by default; add database routines when there is a clear technical or business reason.
- Version changes through migration files rather than editing deployed routines outside the migration process.
