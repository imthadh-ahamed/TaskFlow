---
description: Review frontend changes for correctness, accessibility, security, and maintainability
---

Review the current frontend diff.

1. Inspect the diff and surrounding components/configuration.
2. Check for runtime or type errors, incorrect App Router usage, unnecessary client components, unsafe environment variable exposure, and API contract assumptions.
3. Check accessibility, responsive behavior, error states, and performance concerns relevant to the change.
4. Check dependency and lockfile changes.
5. Do not edit files unless explicitly asked to fix findings.
6. Report findings by severity with file/line references where possible. State what was not verified.
