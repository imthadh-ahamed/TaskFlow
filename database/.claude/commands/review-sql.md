---
description: Review SQL changes for correctness, safety, performance, and maintainability
---

Review the current database SQL diff.

1. Inspect changed SQL and relevant migrations.
2. Check syntax assumptions, object dependencies, constraints, naming, privileges, and transaction behavior.
3. Look for unsafe dynamic SQL, untrusted input concatenation, overly broad destructive statements, and sensitive data leakage.
4. Consider migration ordering, locks, data volume, index impact, and recovery implications.
5. Do not execute SQL or modify files unless the user explicitly asks.
6. Report findings by severity with file/line references where possible and clearly state what was not run against PostgreSQL.
