---
name: write-tests
description: Create or improve focused automated tests for the TaskFlow Spring Boot backend.
---

# Write backend tests

1. Inspect current dependencies and existing test conventions.
2. Identify observable behavior and the most valuable edge cases.
3. Prefer clear arrange/act/assert structure without excessive mocking.
4. Mock external collaborators for unit tests; use integration tests when Spring wiring, HTTP behavior, or persistence is the target.
5. Keep tests deterministic and independent of execution order.
6. Avoid testing private implementation details when public behavior can be tested.
7. Do not introduce new test libraries without explaining why.
8. Run targeted tests, then broader tests when practical. Report commands and results honestly.
