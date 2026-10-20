# SQL naming and file layout

- Flyway migration examples: `V1__initial_schema.sql`, `V2__create_indexes.sql`.
- Use descriptive, lowercase `snake_case` names for SQL objects unless the project has an established convention.
- Store versioned schema changes in `migrations/`.
- Store procedure source or organization notes in `procedures/`; deployed changes must still be captured in migrations.
- Store reusable functions in `functions/`, views in `views/`, seed data in `seed/`, and test assets in `tests/`.
- Store query benchmarks and `EXPLAIN` notes in `performance/`.
- Avoid duplicating the same authoritative SQL definition across multiple folders. Make migration history the deployment source of truth.
