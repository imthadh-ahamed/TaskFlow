---
description: Run appropriate backend tests and summarize verified results
---

Inspect the changed code and existing test setup, then run the most relevant tests.

- Prefer the Maven Wrapper: `./mvnw test` (Windows: `mvnw.cmd test`).
- Run targeted tests first when the changed area is clear, then the full test suite when practical.
- Do not modify tests or production code merely to hide failures.
- If tests fail, summarize the first meaningful failure and likely cause, distinguishing diagnosis from certainty.
- Report exact commands and whether they passed, failed, or could not run.
