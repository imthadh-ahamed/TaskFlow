# seed/

SQL scripts that populate the database with reference data or development fixtures.

## Types of Seed Scripts

| Type             | Purpose                                      | Applied in            |
|------------------|----------------------------------------------|-----------------------|
| Reference data   | Lookup tables, static enumerations           | All environments      |
| Development data | Sample tasks, users, and projects for testing| Local / staging only  |

## Rules

- Reference data that belongs to every environment should be introduced through a Flyway migration (e.g. `V5__seed_reference_data.sql`).
- Development-only seed scripts should **never** be applied to production.
- Scripts must be idempotent where possible (`INSERT ... ON CONFLICT DO NOTHING`).
- Never include real personal data, production credentials, or sensitive information.

No seed files exist yet.
