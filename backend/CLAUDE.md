# TaskFlow Backend — Claude Code Guide

## Project context
This repository is the Spring Boot backend for TaskFlow, a learning project in a monorepo:
- `frontend/`: Next.js + React + TypeScript
- `backend/`: Java + Spring Boot REST API (this directory)
- `database/`: PostgreSQL schema, migrations, seed data, and SQL assets

This archive contains the backend directory. If working inside the full monorepo, preserve the sibling boundaries above.

## Current baseline
- Java 21
- Maven Wrapper (`./mvnw`; Windows: `mvnw.cmd`)
- Spring Boot version is defined in `pom.xml`
- Spring MVC starter is currently installed
- The initial application package is `com.example.backend`
- `application.properties` currently contains only the application name

Do not assume PostgreSQL, Spring Security, JPA, Flyway, Redis, Kafka, or OpenAPI are already configured. Inspect `pom.xml` before using any library. Propose dependency changes before introducing a new infrastructure component.

## Working principles
1. Inspect the relevant code and tests before editing.
2. Prefer the smallest complete change that solves the requested problem.
3. Keep business logic out of controllers; use clear application/service boundaries as the project grows.
4. Validate external input and return stable response DTOs rather than exposing persistence entities.
5. Use constructor injection; avoid field injection and global mutable state.
6. Preserve backward compatibility unless the task explicitly allows a breaking change.
7. Never log passwords, tokens, session identifiers, secrets, or sensitive personal data.
8. Never commit secrets. Use environment variables or an approved secret manager for runtime secrets.
9. Do not add microservices, Kafka, Redis, caching, or other infrastructure merely for fashion. Add them when the task or a measurable requirement calls for them.
10. Keep database SQL and migration ownership in the `database/` component when working in the full monorepo. Do not connect the frontend directly to the database.

## Before coding
- Identify the requested behavior and acceptance criteria.
- Read relevant source files, `pom.xml`, configuration, and tests.
- Check whether the change affects API contracts, security, persistence, or migrations.
- If requirements are ambiguous in a way that changes the architecture or API, ask a concise clarification; otherwise state a reasonable assumption.

## Implementation standards
- Use descriptive names and focused classes/methods.
- Prefer immutable values where practical.
- Use Java records for simple request/response DTOs when suitable.
- Use Jakarta Bean Validation for request validation when the validation dependency is present; add it only when needed.
- Keep HTTP concerns in controllers and business rules in application/domain code.
- Use explicit transaction boundaries when persistence is introduced.
- Handle errors centrally once the API grows; do not expose stack traces or internal details to clients.
- Use pagination and bounded result sets for collection endpoints.
- Avoid premature abstraction and unnecessary dependencies.

## Testing and verification
- Add or update tests for behavior changes and regression risks.
- Prefer unit tests for isolated logic and integration tests for framework, database, or HTTP behavior.
- Run `./mvnw test` (Windows: `mvnw.cmd test`) after meaningful changes when the environment allows it.
- Run `./mvnw verify` for broader verification when appropriate.
- If a command cannot run, report the exact reason; never claim tests passed without running them.
- Do not delete or weaken tests just to make a build pass.

## Safe tool use
- Read-only inspection and targeted edits are preferred.
- Do not run destructive commands, delete broad directories, reset Git history, force-push, or overwrite user work without explicit permission.
- Do not install system-wide tools or change cloud resources without explicit permission.
- Ask before running commands that incur cloud costs, alter shared environments, rotate credentials, or destroy data.

## Completion response
Summarize:
1. What changed
2. Important design decisions
3. Tests/commands actually run and their results
4. Any remaining risks or follow-up steps
