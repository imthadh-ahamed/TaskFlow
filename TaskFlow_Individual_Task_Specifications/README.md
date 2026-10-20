# TaskFlow — Individual Task Specifications

This ZIP contains one Markdown file per task extracted from the latest TaskFlow plan:
`TaskFlow_Zero_to_End_Task_Plan_JWT_Local_Redis_Kafka.md`.

## How to use
1. Start Claude Code from the TaskFlow monorepo root.
2. Ask Claude to read all applicable root/frontend/backend/database Claude setup Markdown files.
3. Give Claude one task file at a time. Avoid asking it to implement the entire plan in one change.
4. Have Claude update the task's **Verification** section with commands actually run and results.
5. Mark acceptance criteria only after verifying them.

## Task files

- [`T0.1` — Inspect the monorepo](tasks/phase-00/T0.1.md)
- [`T0.2` — Define the shared project contract](tasks/phase-00/T0.2.md)
- [`T0.4` — Prepare local Docker infrastructure for PostgreSQL, Redis, and Kafka](tasks/phase-00/T0.4.md)
- [`T1.1` — Verify the Next.js scaffold](tasks/phase-01/T1.1.md)
- [`T1.2` — Establish UI foundations](tasks/phase-01/T1.2.md)
- [`T2.1` — Verify the Spring Boot scaffold](tasks/phase-02/T2.1.md)
- [`T2.2` — Establish backend configuration conventions](tasks/phase-02/T2.2.md)
- [`T3.1` — Establish SQL folder and naming conventions](tasks/phase-03/T3.1.md)
- [`T3.2` — Plan migration execution](tasks/phase-03/T3.2.md)
- [`T4.1` — Design the initial relational model](tasks/phase-04/T4.1.md)
- [`T4.2` — Create the first schema migration](tasks/phase-04/T4.2.md)
- [`T4.3` — Connect Spring Boot to PostgreSQL](tasks/phase-04/T4.3.md)
- [`T5.1` — Establish feature-oriented backend packages](tasks/phase-05/T5.1.md)
- [`T5.2` — Implement the task domain model](tasks/phase-05/T5.2.md)
- [`T6.1` — Implement project endpoints](tasks/phase-06/T6.1.md)
- [`T6.2` — Implement task endpoints](tasks/phase-06/T6.2.md)
- [`T6.3` — Implement comments and membership APIs](tasks/phase-06/T6.3.md)
- [`T7.1` — Create shared application layout](tasks/phase-07/T7.1.md)
- [`T7.2` — Build project list and project detail pages](tasks/phase-07/T7.2.md)
- [`T7.3` — Build task board and task editor](tasks/phase-07/T7.3.md)
- [`T8.1` — Establish typed API integration](tasks/phase-08/T8.1.md)
- [`T8.2` — Add API documentation](tasks/phase-08/T8.2.md)
- [`T9.1` — Define the JWT and cookie security design](tasks/phase-09/T9.1.md)
- [`T9.2` — Add authentication schema and migrations](tasks/phase-09/T9.2.md)
- [`T9.3` — Implement registration, login, and cookie issuance](tasks/phase-09/T9.3.md)
- [`T9.4` — Implement refresh rotation, logout, and revocation](tasks/phase-09/T9.4.md)
- [`T9.5` — Enforce authorization on protected resources](tasks/phase-09/T9.5.md)
- [`T9.6` — Integrate cookie-based authentication in the frontend](tasks/phase-09/T9.6.md)
- [`T9.7` — Complete authentication security verification](tasks/phase-09/T9.7.md)
- [`T10.1` — Add indexes based on query patterns](tasks/phase-10/T10.1.md)
- [`T10.2` — Implement pagination and filtering efficiently](tasks/phase-10/T10.2.md)
- [`T10.3` — Add one meaningful stored procedure exercise](tasks/phase-10/T10.3.md)
- [`T10.4` — Learn query and ORM behavior](tasks/phase-10/T10.4.md)
- [`T11.1` — Standardize backend errors](tasks/phase-11/T11.1.md)
- [`T11.2` — Improve frontend resilience](tasks/phase-11/T11.2.md)
- [`T12.1` — Backend unit tests](tasks/phase-12/T12.1.md)
- [`T12.2` — Backend integration tests](tasks/phase-12/T12.2.md)
- [`T12.3` — Frontend tests](tasks/phase-12/T12.3.md)
- [`T12.4` — End-to-end acceptance flow](tasks/phase-12/T12.4.md)
- [`T13.1` — Measure before caching](tasks/phase-13/T13.1.md)
- [`T13.2` — Add Redis caching](tasks/phase-13/T13.2.md)
- [`T13.3` — Design notification events](tasks/phase-13/T13.3.md)
- [`T13.4` — Add Kafka notification processing](tasks/phase-13/T13.4.md)
- [`T14.1` — Add health and operational visibility](tasks/phase-14/T14.1.md)
- [`T14.2` — Add Docker-based local development](tasks/phase-14/T14.2.md)
- [`T14.3` — Add CI checks](tasks/phase-14/T14.3.md)
- [`T14.4` — Prepare deployment configuration](tasks/phase-14/T14.4.md)
- [`T15.1` — Security and quality review](tasks/phase-15/T15.1.md)
- [`T15.2` — Documentation and architecture review](tasks/phase-15/T15.2.md)
