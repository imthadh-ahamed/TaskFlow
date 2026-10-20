# tests/

Database-level integration and unit tests.

## Approach

- Tests verify that migrations, procedures, functions, and views behave correctly against a real PostgreSQL instance.
- A clean, isolated test database should be used; never run tests against a shared or production database.
- Tests may use plain SQL assertions or a framework such as pgTAP.

## Prerequisites

- A running local PostgreSQL instance with a test database provisioned.
- Environment variables set from `.env` (see `.env.example`).

## Structure (planned)

```
tests/
├── migrations/     # Verify migration scripts apply cleanly
├── procedures/     # Test stored procedure input/output and error handling
└── functions/      # Test function return values and edge cases
```

No test files exist yet.
