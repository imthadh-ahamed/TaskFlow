# Task Manifest

| Task ID | Phase | Title | Owner | Priority |
|---|---:|---|---|---|
| T0.1 | 0 | Inspect the monorepo | Root | Required |
| T0.2 | 0 | Define the shared project contract | Root | Required |
| T0.4 | 0 | Prepare local Docker infrastructure for PostgreSQL, Redis, and Kafka | Database | Required for the local development environment |
| T1.1 | 1 | Verify the Next.js scaffold | Frontend | Required |
| T1.2 | 1 | Establish UI foundations | Frontend | Required before feature pages |
| T2.1 | 2 | Verify the Spring Boot scaffold | Backend | Required |
| T2.2 | 2 | Establish backend configuration conventions | Backend | Required before persistence |
| T3.1 | 3 | Establish SQL folder and naming conventions | Database | Required |
| T3.2 | 3 | Plan migration execution | Backend / Database | Required before schema implementation |
| T4.1 | 4 | Design the initial relational model | Backend / Database | Required |
| T4.2 | 4 | Create the first schema migration | Database | Required |
| T4.3 | 4 | Connect Spring Boot to PostgreSQL | Backend / Database | Required |
| T5.1 | 5 | Establish feature-oriented backend packages | Backend | Required |
| T5.2 | 5 | Implement the task domain model | Backend | Required |
| T6.1 | 6 | Implement project endpoints | Backend | Required |
| T6.2 | 6 | Implement task endpoints | Backend | Required |
| T6.3 | 6 | Implement comments and membership APIs | Backend / Database | Required for collaboration release |
| T7.1 | 7 | Create shared application layout | Frontend | Required |
| T7.2 | 7 | Build project list and project detail pages | Frontend | Required |
| T7.3 | 7 | Build task board and task editor | Frontend | Required |
| T8.1 | 8 | Establish typed API integration | Frontend / Backend / Database | Required |
| T8.2 | 8 | Add API documentation | Backend | Required |
| T9.1 | 9 | Define the JWT and cookie security design | Frontend / Backend / Database | Required before real user data is exposed |
| T9.2 | 9 | Add authentication schema and migrations | Backend / Database | Required |
| T9.3 | 9 | Implement registration, login, and cookie issuance | Backend | Required |
| T9.4 | 9 | Implement refresh rotation, logout, and revocation | Backend / Database | Required |
| T9.5 | 9 | Enforce authorization on protected resources | Backend | Required |
| T9.6 | 9 | Integrate cookie-based authentication in the frontend | Frontend | Required |
| T9.7 | 9 | Complete authentication security verification | Frontend / Backend / Database | Required before public deployment |
| T10.1 | 10 | Add indexes based on query patterns | Backend / Database | Required before performance tuning |
| T10.2 | 10 | Implement pagination and filtering efficiently | Backend / Database | Required |
| T10.3 | 10 | Add one meaningful stored procedure exercise | Backend / Database | Learning objective; not required for basic CRUD |
| T10.4 | 10 | Learn query and ORM behavior | Backend / Database | Required learning stage |
| T11.1 | 11 | Standardize backend errors | Backend | Required |
| T11.2 | 11 | Improve frontend resilience | Frontend | Required |
| T12.1 | 12 | Backend unit tests | Backend | Required |
| T12.2 | 12 | Backend integration tests | Backend / Database | Required |
| T12.3 | 12 | Frontend tests | Frontend | Required for critical flows |
| T12.4 | 12 | End-to-end acceptance flow | Frontend / Backend / Database | Required |
| T13.1 | 13 | Measure before caching | Backend / Database | Optional advanced learning |
| T13.2 | 13 | Add Redis caching | Backend | Optional advanced learning |
| T13.3 | 13 | Design notification events | Backend / Database | Optional advanced learning |
| T13.4 | 13 | Add Kafka notification processing | Backend | Optional advanced learning |
| T14.1 | 14 | Add health and operational visibility | Backend | Required before deployment |
| T14.2 | 14 | Add Docker-based local development | Backend / Database | Recommended |
| T14.3 | 14 | Add CI checks | Root | Recommended |
| T14.4 | 14 | Prepare deployment configuration | Root | Final release stage |
| T15.1 | 15 | Security and quality review | Root / Frontend / Backend / Database | Required |
| T15.2 | 15 | Documentation and architecture review | Root / Frontend / Backend / Database | Required |
