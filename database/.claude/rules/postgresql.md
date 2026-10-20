# PostgreSQL rules

- Use PostgreSQL SQL and PL/pgSQL.
- Inspect the repository's existing conventions before adding new SQL assets.
- Prefer explicit column lists, clear aliases, and predictable naming.
- Use constraints to protect data integrity where appropriate.
- Schema-qualify objects when it prevents ambiguity or search-path risks.
- Use parameterized values; never concatenate untrusted input into SQL.
- Avoid `SELECT *` in durable scripts and stored routines.
- Do not add tables, indexes, triggers, functions, or procedures unless required by the user's request.
- Do not assume a live database is available.
