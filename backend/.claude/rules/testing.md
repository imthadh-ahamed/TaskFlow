# Testing rules

- Add or update tests for each meaningful behavior change.
- Use JUnit tests for business logic and Spring Boot integration tests when framework wiring or HTTP behavior matters.
- Test both successful and relevant failure paths, including invalid input and authorization failures where applicable.
- Keep tests deterministic; avoid real external services unless the test is explicitly an integration test.
- Prefer test data local to each test and avoid order-dependent tests.
- Do not change production behavior only to satisfy an overly coupled test; improve the design or test boundary.
- Never remove, skip, or weaken an existing test merely to obtain a green build.
- Report commands actually executed and distinguish skipped tests from passing tests.
