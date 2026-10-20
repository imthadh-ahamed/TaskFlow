# Database migration rules

- Use Flyway-compatible versioned migration names, for example `V1__initial_schema.sql`.
- Use a double underscore between the version and description.
- Keep migrations ordered and make dependencies explicit.
- Do not modify migrations already applied to a shared environment; add a new migration instead.
- Review schema changes for data integrity, locking, execution time, and recovery implications.
- Do not run migrations against shared or production environments without explicit authorization.
- Keep SQL assets in `database/`; the backend invokes Flyway when configured.
- Do not assume Flyway is installed or configured until the backend build and configuration confirm it.
