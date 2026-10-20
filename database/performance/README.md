# performance/

SQL scripts for query performance analysis, index evaluation, and benchmarking.

## Contents

- `EXPLAIN` / `EXPLAIN ANALYZE` plans for critical queries
- Index coverage analysis queries
- Slow-query investigation scripts
- Benchmark scripts for load or volume testing

## Rules

- Scripts here are read-only analytical tools, not schema changes.
- Never run `EXPLAIN ANALYZE` on large tables in production without understanding the I/O impact.
- Schema changes motivated by performance findings go through versioned migrations in `migrations/`.

No performance scripts exist yet.
