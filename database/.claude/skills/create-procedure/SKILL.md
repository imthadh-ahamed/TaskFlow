---
name: create-procedure
description: Design and implement a PostgreSQL stored procedure when explicitly requested.
---

# Create a PostgreSQL stored procedure

1. Inspect the schema and current procedure conventions; do not invent tables or columns.
2. Confirm purpose, parameters, outputs, permissions, side effects, and transaction assumptions.
3. Decide whether a procedure is actually needed or whether a query, function, or backend service is more appropriate.
4. Use PostgreSQL SQL/PL/pgSQL with explicit parameter types and safe object references.
5. Avoid unsafe dynamic SQL and unnecessary `SECURITY DEFINER`.
6. Capture the procedure definition in a versioned migration; avoid maintaining conflicting duplicate definitions.
7. Add documentation and tests for normal behavior, edge cases, and errors where appropriate.
8. Do not execute against shared or production databases without explicit authorization.
9. Report what was implemented and what was actually tested.
