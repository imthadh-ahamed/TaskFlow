# Database and migration rules

- The `database/` component owns schema migrations, seed data, and standalone SQL assets in the TaskFlow monorepo.
- The backend owns application persistence logic and the configuration that invokes migrations.
- Never let the frontend connect directly to PostgreSQL.
- Prefer explicit primary keys, foreign keys, uniqueness constraints, and not-null constraints where the domain requires them.
- Design indexes around measured/common query patterns; do not add speculative indexes without explaining the trade-off.
- Use parameterized queries; never concatenate untrusted input into SQL.
- Treat migrations as forward-only and reviewable. Do not rewrite migrations already applied to shared environments without explicit approval.
- Do not run destructive migrations or data deletion against a shared or production database without explicit approval.
- Do not assume PostgreSQL, JPA, or Flyway is configured until the build and configuration confirm it.
