# Scope rules for database-only work

- Work only inside `database/` unless the user explicitly authorizes broader changes.
- Do not modify frontend or backend files for database setup tasks.
- Do not create business tables, stored procedures, functions, views, triggers, or indexes during initial boilerplate setup.
- Do not install software, create containers, connect to external services, or provision cloud resources without permission.
- Do not initialize a nested Git repository.
- Preserve existing files and ask before overwriting them.
