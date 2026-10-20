# Database testing rules

- Inspect the available test setup before choosing a test strategy.
- Do not claim SQL was tested unless it was executed against PostgreSQL or validated by a clearly identified tool.
- Test migrations in a disposable database before using them in shared environments.
- For stored routines, test normal behavior, boundary inputs, invalid inputs, error handling, and permissions as appropriate.
- Keep tests repeatable and isolate test data.
- Never run destructive tests against production or a shared database.
- Do not introduce a test framework or database dependency without a clear requirement and approval.
- Report exact commands and actual results; state what remains unverified.
