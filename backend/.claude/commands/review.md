---
description: Review backend changes for correctness, security, maintainability, and test coverage
---

Review the current changes in this Spring Boot backend.

1. Inspect the diff and the surrounding code.
2. Look for correctness defects, broken API contracts, authorization gaps, unsafe input handling, secret leakage, transaction issues, and performance risks.
3. Check whether controllers contain business logic or persistence entities are exposed as API responses.
4. Check whether tests cover changed behavior and important failure paths.
5. Do not modify files unless the user explicitly asks for fixes.
6. Report findings by severity with file/line references where possible. If no issues are found, state what was reviewed and what was not verified.
