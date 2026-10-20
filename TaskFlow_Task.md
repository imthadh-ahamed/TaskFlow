# TaskFlow — Zero-to-End Implementation Plan

**Project type:** Full-stack task and project management application  
**Repository model:** One Git monorepo with separate `frontend/`, `backend/`, and `database/` components  
**Purpose:** Build a production-minded learning project in small, verifiable stages, from an empty scaffold to a tested and deployable application.

---

## 1. Project vision

TaskFlow is a task and project management application inspired by a simplified Trello. Users can register, create projects, manage project members, create and assign tasks, move tasks through statuses, comment on tasks, search and filter work, and view project/dashboard summaries.

The application will begin as a **modular monolith**. The frontend, backend, and database have clear responsibilities, but the backend is initially one deployable Spring Boot application. **Redis and Apache Kafka will run locally in Docker** as development infrastructure. They can be started and verified early, but TaskFlow application code should integrate them only in the relevant later tasks after the core workflows work.

### Final capabilities

- User registration and JWT authentication using a short-lived access token and long-lived, rotating refresh token
- Roles and project-level authorization
- Projects, memberships, tasks, comments, priorities, and due dates
- Search, filtering, sorting, and pagination
- Responsive web UI and accessible interactions
- Dashboard statistics
- PostgreSQL schema, constraints, and versioned migrations
- Stored procedure/function exercises where they provide a clear benefit
- API documentation
- Automated tests at multiple levels
- Optional Redis caching and rate limiting
- Optional Kafka-based asynchronous notifications
- Logging, metrics, Docker-based local setup, CI, and deployment documentation

### Explicit non-goals for the first release

- Microservices
- Kubernetes
- Multi-tenant enterprise administration
- Real-time collaboration/websocket updates
- Payment processing
- Native mobile apps
- Integrating Redis or Kafka into application features before core CRUD, security, and tests work (running the local containers is allowed)

These can be future extensions rather than prerequisites for the first complete release.

---

## 2. Repository and ownership boundaries

```text
TaskFlow/
├── frontend/                 # Next.js, React, TypeScript, Tailwind
├── backend/                  # Java, Spring Boot, REST API
├── database/                 # PostgreSQL SQL assets and database docs
├── docker-compose.yml        # Local PostgreSQL, Redis, and Kafka services
├── .gitignore
└── README.md
```

### Component responsibilities

| Component | Owns | Must not do |
|---|---|---|
| `frontend/` | Pages, UI components, client interactions, typed API integration, frontend validation | Connect directly to PostgreSQL or implement authoritative access control |
| `backend/` | REST API, business rules, authentication/authorization, orchestration, persistence integration, API error contracts | Put UI concerns in backend or assume frontend visibility is security |
| `database/` | Versioned SQL migrations, schema documentation, seed data, procedures/functions/views, database test and performance assets | Become a second, undocumented source of business logic |
| Monorepo root | Shared documentation, Git, and later local orchestration | Contain component-specific implementation without a clear reason |

**Source of truth:** PostgreSQL stores durable application data. The backend enforces business rules and authorization. The frontend presents the data and calls the backend through HTTP APIs.

### Git rules

- Use one Git repository at the `TaskFlow/` root.
- Do not run `git init` inside the component directories.
- Keep each component's dependencies and build scripts separate.
- Changes spanning multiple components must update their contracts and documentation together.

---

## 3. Technology baseline

Use the versions already selected in the initialized project where possible. Before changing versions, inspect the actual manifests and verify compatibility.

| Area | Baseline |
|---|---|
| Frontend | Next.js App Router, React, TypeScript strict mode, Tailwind CSS, ESLint, npm |
| Backend | Java 21, Spring Boot, Maven, Spring Web |
| Persistence | PostgreSQL, Spring Data JPA/Hibernate when added |
| Migrations | Flyway-compatible versioned SQL assets |
| Validation | Jakarta Bean Validation when configured |
| Security | Spring Security with JWT access/refresh tokens, secure HttpOnly cookies, refresh-token rotation/revocation, and CSRF protection |
| API documentation | OpenAPI/Swagger when core endpoints stabilize |
| Backend tests | JUnit 5 and Spring Boot Test; Mockito where useful |
| Integration tests | Testcontainers if suitable and approved |
| Local cache service | Redis in Docker, exposed on host port `6379`; application caching is integrated later |
| Local messaging service | Apache Kafka in Docker, exposed on host port `9092`; producers/consumers are implemented later |
| Local orchestration | Docker Compose for PostgreSQL, Redis, and Kafka, with named volumes and health checks |
| Observability | Spring Boot Actuator and structured logs, then metrics |

**Important:** Do not assume a dependency exists because it appears in this plan. Check `frontend/package.json`, `backend/pom.xml`, and actual configuration first. Add dependencies in the stage where they are needed.

---

## 4. How to execute each task

Every task below has an owner component, a requirement, implementation guidance, dependencies, and acceptance criteria.

For each task:

1. Read the relevant `CLAUDE.md`, rules, and current code.
2. Confirm the task scope before editing.
3. Implement only that task and necessary supporting changes.
4. Add or update tests and documentation.
5. Run the stated checks.
6. Review the diff and confirm unrelated components were not modified.
7. Mark the task complete only when its acceptance criteria are met.

Do not ask an AI coding assistant to implement every phase in one giant change. Work in small tasks and verify each result.

---

# Phase 0 — Repository baseline and project agreement

## T0.1 — Inspect the monorepo

**Owner:** Root / all components  
**Priority:** Required

**Requirement:** Confirm the current repository structure, component initialization state, available scripts, runtime versions, and existing Claude Code instructions before implementation.

**Work:**
- Inspect root folders and Git status.
- Inspect `frontend/package.json`, lockfile, and Next.js configuration.
- Inspect `backend/pom.xml`, Java/Maven Wrapper configuration, and application entry point.
- Inspect the `database/` structure and any existing SQL files.
- Record current versions and any missing prerequisites.
- Do not overwrite or regenerate initialized components.

**Acceptance criteria:**
- The three components are present.
- Existing user changes are preserved.
- A short baseline report identifies available commands and missing dependencies.
- No application behavior is changed.

## T0.2 — Define the shared project contract

**Owner:** Root + frontend + backend + database  
**Priority:** Required

**Requirement:** Agree on naming, API conventions, environment variables, error formats, and data ownership before components are implemented independently.

**Work:**
- Define the local frontend and backend URLs.
- Define API prefix `/api/v1`.
- Define a consistent JSON error response shape.
- Document which component owns migrations and how the backend will load them.
- Document how environment variables are supplied without committing secrets.
- Keep the contract concise and update it when the implementation changes.

**Acceptance criteria:**
- A root README explains how the components communicate.
- The frontend does not connect to PostgreSQL.
- Database migrations have one documented owner.
- API and configuration assumptions are written down.

---

## T0.4 — Prepare local Docker infrastructure for PostgreSQL, Redis, and Kafka

**Owner:** Monorepo root + Database  
**Priority:** Required for the local development environment

**Requirement:** Run Redis and Apache Kafka locally in Docker, similar to the planned local PostgreSQL service, without prematurely coupling application code to them.

**Work:**
- Add or update the root `docker-compose.yml` to define local development services for PostgreSQL, Redis, and Apache Kafka in KRaft mode (no ZooKeeper).
- Publish Redis on host port `6379` and Kafka on host port `9092`; document any additional internal controller/listener ports needed by the chosen Kafka image.
- Configure Kafka advertised listeners correctly for the expected clients: host-run applications use `localhost:9092`; containers in the same Compose network use the Kafka service name and its internal listener.
- Use pinned, compatible image versions rather than `latest`; record the chosen image versions in the README.
- Add named volumes for data persistence and health checks where supported.
- Keep development credentials and secrets out of source control. Use local-only values and `.env.example` placeholders when needed.
- Document commands to start, inspect logs, stop, and reset services. Clearly warn before any command that deletes volumes/data.
- Verify Redis responds to `PING`, Kafka starts successfully, and a test Kafka topic can be created/listed.
- Do not add Redis caching, Kafka producers/consumers, or application dependencies in this task; those belong to later tasks.

**Acceptance criteria:**
- `docker compose up -d` starts the documented local services successfully.
- Redis is reachable from the host at `localhost:6379`.
- Kafka is reachable from the host at `localhost:9092` and works with the documented client address.
- PostgreSQL readiness and connection details are documented.
- Data persistence, shutdown, and destructive reset behavior are clear.
- A clean checkout can follow the documented steps without requiring production credentials.

---

# Phase 1 — Frontend boilerplate

## T1.1 — Verify the Next.js scaffold

**Owner:** Frontend  
**Priority:** Required

**Requirement:** Produce a runnable, minimal Next.js application without implementing TaskFlow features.

**Work:**
- Verify App Router, TypeScript strict mode, Tailwind CSS, ESLint, and npm.
- Confirm the `@/*` import alias works.
- Create only useful organizational folders, such as `components/`, `features/`, `lib/`, `hooks/`, `types/`, and `constants/`.
- Add a minimal `.env.example` with a placeholder API base URL.
- Add a frontend README with install, dev, lint, and build commands.
- Preserve generated files and avoid duplicate configuration.

**Acceptance criteria:**
- Dependencies install from the lockfile.
- Lint and production build pass.
- The app starts locally.
- No authentication, dashboard, API client, or business feature is implemented.

## T1.2 — Establish UI foundations

**Owner:** Frontend  
**Priority:** Required before feature pages

**Requirement:** Set minimal global UI conventions so later pages are consistent without prematurely building a design system.

**Work:**
- Set global typography and base styles.
- Define a small set of reusable layout conventions.
- Ensure the placeholder page is responsive and uses semantic HTML.
- Do not add a component library unless a concrete requirement appears.

**Acceptance criteria:**
- The page is usable on mobile and desktop widths.
- Keyboard focus is visible.
- There are no console errors caused by the scaffold.
- No unrequested features or dependencies are introduced.

---

# Phase 2 — Backend boilerplate

## T2.1 — Verify the Spring Boot scaffold

**Owner:** Backend  
**Priority:** Required

**Requirement:** Ensure the Java application builds and starts before business code is added.

**Work:**
- Verify Java version, Maven Wrapper, Spring Boot version, and source package.
- Confirm the application entry point and configuration file.
- Add a concise backend README with build, test, and run commands.
- Do not add database, security, Redis, Kafka, or OpenAPI dependencies unless required by this task.

**Acceptance criteria:**
- Maven Wrapper build succeeds.
- Existing tests run.
- Application starts locally.
- No business endpoint is added solely for demonstration.

## T2.2 — Establish backend configuration conventions

**Owner:** Backend  
**Priority:** Required before persistence

**Requirement:** Keep configuration environment-aware and safe.

**Work:**
- Document local and production configuration expectations.
- Keep secrets out of committed configuration.
- Use environment variables or a proper secret provider for sensitive settings.
- Define logging conventions and a future health-check approach.
- Avoid committing live credentials.

**Acceptance criteria:**
- README explains required configuration.
- `.env.example` or equivalent contains placeholders only.
- No secrets are present in Git-tracked files.

---

# Phase 3 — Database boilerplate

## T3.1 — Establish SQL folder and naming conventions

**Owner:** Database  
**Priority:** Required

**Requirement:** Organize database assets before schema development.

**Work:**
- Maintain `migrations/`, `procedures/`, `functions/`, `views/`, `seed/`, `tests/`, `performance/`, and `docs/`.
- Document PostgreSQL SQL and PL/pgSQL conventions.
- Document Flyway migration naming such as `V1__initial_schema.sql`.
- Add `.env.example` placeholders for local database connection details.
- Ignore local secret files.
- Do not create application tables or routines yet.

**Acceptance criteria:**
- The structure and naming conventions are documented.
- No real credentials exist.
- No database objects are created as part of boilerplate setup.

## T3.2 — Plan migration execution

**Owner:** Database + Backend  
**Priority:** Required before schema implementation

**Requirement:** Establish how database SQL assets become part of a repeatable application setup.

**Work:**
- Decide how SQL assets from `database/` will be packaged or made available to Flyway.
- Choose a repeatable build/deployment approach; do not rely on a developer's arbitrary local filesystem path.
- Document how a new developer creates a disposable local database.
- Decide when migrations run locally, in tests, and in deployment.
- Avoid automatically running destructive scripts.

**Acceptance criteria:**
- The approach is documented.
- Backend configuration is not assumed until implemented and tested.
- A later migration can be executed consistently from a clean database.

---

# Phase 4 — Data model and first migration

## T4.1 — Design the initial relational model

**Owner:** Database, reviewed by Backend  
**Priority:** Required

**Requirement:** Define the smallest schema that supports users, projects, memberships, and tasks without prematurely modeling every future feature.

**Initial entities:**
- `users`: user identity and account status; password hashes are added only with authentication.
- `projects`: project name, owner, timestamps.
- `project_members`: project/user relationship and membership role.
- `tasks`: project, optional assignee, title, description, status, priority, due date, timestamps.

**Work:**
- Define primary keys, foreign keys, nullability, uniqueness, and status constraints.
- Decide how email normalization and uniqueness are handled.
- Define timestamps and consistent time-zone handling.
- Document deletion behavior for relationships.
- Review whether project ownership is represented by a foreign key and whether owners are also members.
- Avoid adding comments, notifications, audit logs, or outbox tables until their stages.

**Acceptance criteria:**
- An ER diagram or equivalent relationship description exists.
- Entity definitions and constraints are reviewed.
- Important integrity rules are enforced by the database where appropriate.
- No migration is applied to a shared database.

## T4.2 — Create the first schema migration

**Owner:** Database  
**Priority:** Required

**Requirement:** Turn the approved data model into a versioned, repeatable PostgreSQL migration.

**Work:**
- Create the first Flyway-compatible migration.
- Use explicit column definitions and constraints.
- Keep naming consistent.
- Test it against a disposable PostgreSQL database if available.
- Do not put real passwords or sensitive sample data in seed scripts.

**Acceptance criteria:**
- The migration applies to a clean local database.
- Constraints and foreign keys behave as expected.
- The migration is tracked in Git.
- The backend's migration-loading method is documented or verified.

## T4.3 — Connect Spring Boot to PostgreSQL

**Owner:** Backend + Database  
**Priority:** Required

**Requirement:** Allow the backend to access PostgreSQL using explicit, environment-driven configuration.

**Work:**
- Add the PostgreSQL driver and persistence dependencies when needed.
- Configure a local datasource using environment variables.
- Configure Flyway to load the approved migrations.
- Add connection-pool configuration only when necessary, with safe defaults.
- Use a least-privilege application database role.
- Keep migration privileges separate from normal application privileges where practical.

**Acceptance criteria:**
- Backend starts with a disposable local database.
- Migration status is observable and repeatable.
- No credentials are committed.
- Connection failures produce useful logs without exposing secrets.

---

# Phase 5 — Java domain and backend architecture

## T5.1 — Establish feature-oriented backend packages

**Owner:** Backend  
**Priority:** Required

**Requirement:** Organize backend code around capabilities while retaining a single deployable Spring Boot application.

**Suggested structure:**
```text
com.taskflow/
├── auth/
├── project/
│   ├── api/
│   ├── application/
│   ├── domain/
│   └── infrastructure/
├── task/
├── common/
└── config/
```

**Work:**
- Confirm the final base package.
- Keep HTTP handling in API/controller classes.
- Put use-case orchestration in application services.
- Keep business rules separate from transport and persistence concerns.
- Use constructor injection.
- Avoid creating empty abstractions for hypothetical future services.

**Acceptance criteria:**
- Package conventions are documented.
- The application still builds.
- The architecture remains a modular monolith.

## T5.2 — Implement the task domain model

**Owner:** Backend  
**Priority:** Required

**Requirement:** Model task state and basic business rules before adding the full API.

**Work:**
- Define task statuses: `TODO`, `IN_PROGRESS`, and `DONE`.
- Define allowed priority values.
- Validate title length and required fields.
- Define valid status transitions if the product requires restrictions.
- Write unit tests for domain rules.
- Keep persistence annotations out of pure domain classes if the selected architecture calls for that separation; do not force complexity without benefit.

**Acceptance criteria:**
- Invalid values are rejected.
- Domain behavior has automated tests.
- Business rules do not depend on HTTP controllers.

---

# Phase 6 — Backend CRUD API

## T6.1 — Implement project endpoints

**Owner:** Backend  
**Priority:** Required

**Requirement:** Provide versioned REST endpoints to create, list, view, update, and archive/delete projects according to the agreed deletion policy.

**Proposed API:**
- `POST /api/v1/projects`
- `GET /api/v1/projects`
- `GET /api/v1/projects/{projectId}`
- `PATCH /api/v1/projects/{projectId}`
- `DELETE /api/v1/projects/{projectId}` only if the deletion policy permits it

**Work:**
- Define request/response DTOs.
- Validate input.
- Add service-level rules.
- Use appropriate HTTP status codes.
- Add pagination to list endpoints.
- Define consistent error responses.
- For the unauthenticated development stage only, avoid presenting the API as production-secure.

**Acceptance criteria:**
- Endpoints behave as documented.
- Invalid input returns a stable client error.
- Missing resources return an appropriate not-found response.
- Automated tests cover normal and failure paths.

## T6.2 — Implement task endpoints

**Owner:** Backend  
**Priority:** Required

**Requirement:** Support task creation, listing, retrieval, updates, and status changes.

**Proposed API:**
- `POST /api/v1/projects/{projectId}/tasks`
- `GET /api/v1/projects/{projectId}/tasks`
- `GET /api/v1/tasks/{taskId}`
- `PATCH /api/v1/tasks/{taskId}`
- `PATCH /api/v1/tasks/{taskId}/status`

**Work:**
- Validate title, description, status, priority, due date, and assignee fields.
- Enforce that a task belongs to the requested project.
- Add filtering by status, priority, and assignee where appropriate.
- Use bounded pagination and deterministic ordering.
- Do not expose persistence entities directly.

**Acceptance criteria:**
- CRUD and filtering work against PostgreSQL.
- Cross-project references are rejected where invalid.
- Response payloads are documented and tested.

## T6.3 — Implement comments and membership APIs

**Owner:** Backend + Database  
**Priority:** Required for collaboration release

**Requirement:** Allow authorized project members to collaborate through membership management and task comments.

**Work:**
- Design `comments` schema through a new migration.
- Define project membership endpoints and permitted role changes.
- Define comment create/list behavior.
- Prevent users from accessing comments through unrelated projects.
- Document deletion and retention rules.

**Acceptance criteria:**
- New schema changes use new migrations.
- Membership and comment APIs enforce relationship integrity.
- Tests cover unauthorized and invalid membership/comment operations once security is implemented.

---

# Phase 7 — Frontend application shell and core UI

## T7.1 — Create shared application layout

**Owner:** Frontend  
**Priority:** Required

**Requirement:** Create the navigation and page shell without coupling UI components to backend implementation details.

**Work:**
- Add a responsive application shell.
- Add navigation links for dashboard, projects, and tasks as routes are implemented.
- Create small reusable primitives only when repeated use justifies them.
- Use accessible labels, focus states, semantic landmarks, and responsive layouts.
- Keep placeholder routes clearly identified until their features are implemented.

**Acceptance criteria:**
- Navigation works across implemented routes.
- Layout works at mobile and desktop widths.
- No business data is hard-coded as if it came from the backend.

## T7.2 — Build project list and project detail pages

**Owner:** Frontend  
**Priority:** Required

**Requirement:** Let users view projects and navigate to project details.

**Work:**
- Build loading, empty, error, and success states.
- Add project creation/edit forms only when corresponding API endpoints are ready.
- Validate form input for usability; backend validation remains authoritative.
- Keep API requests in a focused module or feature layer.
- Type API responses.

**Acceptance criteria:**
- Pages consume the real backend API.
- API errors are presented accessibly.
- No direct database access exists in frontend code.

## T7.3 — Build task board and task editor

**Owner:** Frontend  
**Priority:** Required

**Requirement:** Provide a clear task view grouped by status, with task creation and editing.

**Work:**
- Build task cards and status columns.
- Add task creation/edit UI.
- Support status and priority display.
- Add filters and pagination where needed.
- Implement drag-and-drop only if explicitly selected; it is not required for the first functional board.
- Avoid adding a state-management library unless local state and API data handling justify it.

**Acceptance criteria:**
- Task board uses backend data.
- Status updates persist through the API.
- Loading, empty, error, and validation states work.
- Keyboard and touch interaction are considered.

---

# Phase 8 — API integration and contracts

## T8.1 — Establish typed API integration

**Owner:** Frontend + Backend  
**Priority:** Required

**Requirement:** Make frontend/backend integration predictable and debuggable.

**Work:**
- Verify actual endpoints and response DTOs.
- Configure a backend base URL through environment variables.
- Define shared frontend types based on the API contract; avoid manually maintaining conflicting definitions.
- Standardize error parsing.
- Handle network failures and expired sessions when authentication is introduced.
- Configure CORS narrowly for local development and later deployment.

**Acceptance criteria:**
- Frontend calls the correct backend URLs.
- Types match actual API payloads.
- Errors are displayed without exposing internal stack traces.
- No secret is placed in a `NEXT_PUBLIC_` variable.

## T8.2 — Add API documentation

**Owner:** Backend  
**Priority:** Required

**Requirement:** Make endpoints understandable and testable by developers.

**Work:**
- Add OpenAPI/Swagger tooling when the endpoint structure is stable.
- Document request bodies, response bodies, status codes, validation errors, pagination, and security requirements.
- Keep docs synchronized with actual API behavior.
- Include local instructions to access the API documentation.

**Acceptance criteria:**
- Implemented endpoints appear in generated API docs.
- Example requests are valid.
- Security behavior and error responses are documented.

---

# Phase 9 — JWT authentication and authorization

## T9.1 — Define the JWT and cookie security design

**Owner:** Backend + Frontend, reviewed together  
**Priority:** Required before real user data is exposed

**Requirement:** Use short-lived JWT access tokens and long-lived refresh tokens, with the browser handling tokens through secure cookies. Make the cookie-based flow consistent across Next.js and Spring Security.

**Work:**
- Use a short-lived access token (initial target: 10–15 minutes) and a longer-lived refresh token (initial target: 7–30 days). Make both durations configurable by environment.
- Use distinct token types/claims or separate signing/validation rules so a refresh token cannot be accepted as an access token.
- Include only necessary claims, such as subject/user ID, token type, issued-at, expiry, issuer, and a unique token ID (`jti`). Do not put passwords, secrets, or sensitive personal data in JWT claims.
- Store access and refresh tokens in `HttpOnly` cookies with `Secure` enabled in production and an intentional `SameSite` policy. Use narrow cookie paths where practical; configure domain only when required.
- Frontend JavaScript must not read token values. The backend sets and clears cookies using `Set-Cookie`; the frontend sends requests with credentials enabled where required.
- Document local-development cookie behavior separately from production HTTPS behavior.
- Define CORS to allow only trusted frontend origins and credentials; never combine credentialed requests with a wildcard origin.
- Because cookies are sent automatically, implement CSRF protection appropriate to the chosen SameSite/cross-origin setup. Include Origin/Referer validation and/or a CSRF token strategy for state-changing requests.
- Keep signing keys and secrets in environment configuration or a secret manager; never commit them. Use a strong, supported signing algorithm and validate signature, issuer, expiry, and token type.
- Define refresh-token rotation, reuse detection, logout/revocation, expired-token behavior, and concurrent refresh behavior.
- Write a concise authentication decision record before implementation.

**Acceptance criteria:**
- The access/refresh lifetimes and cookie attributes are documented.
- The frontend never reads token values from JavaScript.
- CSRF, CORS, XSS, token theft, refresh replay, and signing-key risks have explicit mitigations.
- The same documented contract is followed by the frontend and backend.

## T9.2 — Add authentication schema and migrations

**Owner:** Database + Backend  
**Priority:** Required

**Requirement:** Persist the minimum state needed to support accounts and refresh-token revocation/rotation.

**Work:**
- Add or update the users migration with unique normalized email/username, password hash, role/account status, and timestamps as appropriate.
- Add a refresh-token/session table if using server-tracked refresh tokens. Store a hash of the refresh token or a token identifier, not the raw token; include user ID, expiry, created/rotated/revoked timestamps, and replacement/reuse-detection metadata as appropriate.
- Add foreign keys, uniqueness constraints, expiry/revocation lookup indexes, and cleanup guidance.
- Use Flyway-compatible versioned migrations; do not edit an already-applied migration in place.
- Do not store access tokens in the database unless a documented requirement justifies it.

**Acceptance criteria:**
- A fresh database can migrate successfully.
- Refresh-token records cannot be used to recover raw refresh-token values.
- Constraints and indexes support expiry, rotation, and revocation workflows.
- Migration behavior is documented and tested.

## T9.3 — Implement registration, login, and cookie issuance

**Owner:** Backend  
**Priority:** Required

**Requirement:** Create accounts safely and issue the access/refresh cookie pair after successful authentication.

**Work:**
- Configure Spring Security and password hashing with a modern password encoder; never store plaintext passwords.
- Validate registration and login DTOs and normalize identity fields consistently.
- Implement registration and login endpoints under `/api/v1/auth`.
- On successful login, issue a signed short-lived access JWT and a long-lived refresh token using `HttpOnly` cookies with the agreed attributes.
- Return safe user/session metadata in JSON, but never return token values in JSON, URLs, logs, or error messages.
- Add generic authentication errors that avoid unnecessary account enumeration.
- Add login/registration rate limiting or document a concrete follow-up if it is intentionally deferred.
- Validate JWT signature, issuer, expiry, token type, and required claims on protected requests.
- Use environment-based secrets and fail safely when required production secrets are missing.

**Acceptance criteria:**
- Valid credentials create the expected cookies and safe user response.
- Invalid credentials are rejected with a consistent response.
- Access JWTs expire at the configured short lifetime.
- Refresh tokens cannot authenticate normal API requests.
- Password hashes and token values never appear in responses or logs.
- Tests cover successful and failed registration/login and token validation.

## T9.4 — Implement refresh rotation, logout, and revocation

**Owner:** Backend + Database  
**Priority:** Required

**Requirement:** Renew access without requiring frequent logins, while limiting the damage from a stolen or replayed refresh token.

**Work:**
- Implement a refresh endpoint, for example `POST /api/v1/auth/refresh`, that validates the refresh cookie and its server-side session/token record.
- Rotate the refresh token on successful refresh: invalidate the prior token/session credential and issue a replacement refresh token plus a new short-lived access token.
- Detect refresh-token reuse/replay and revoke the affected token family/session; return a safe authentication error.
- Implement `POST /api/v1/auth/logout` to revoke the active refresh session and clear both cookies using matching path/domain/SameSite attributes.
- Define behavior for expired, missing, malformed, revoked, and reused refresh tokens.
- Make rotation safe against simultaneous refresh requests, using a transaction/locking or another documented strategy.
- Add an authenticated session/current-user endpoint, for example `GET /api/v1/auth/me`, if needed by frontend bootstrapping.
- Never log raw tokens; log only safe identifiers and security events.

**Acceptance criteria:**
- Refreshing a valid session produces a new access token and rotated refresh token.
- The old refresh token cannot be reused successfully.
- Logout revokes the session and clears cookies.
- Expired, revoked, malformed, and replayed refresh tokens are rejected.
- Concurrent refresh behavior is tested and documented.

## T9.5 — Enforce authorization on protected resources

**Owner:** Backend  
**Priority:** Required

**Requirement:** Ensure authenticated users can only access projects and tasks permitted by their membership and role.

**Work:**
- Configure Spring Security to authenticate requests using the access-token cookie.
- Require authentication for protected routes and explicitly define any public routes, such as registration, login, refresh, and health checks.
- Define project owner/admin/member capabilities and read/write rules.
- Enforce authorization in backend services or method security; check access for every resource identifier, not only in the UI.
- Add CSRF checks for state-changing cookie-authenticated requests.
- Return consistent 401 (unauthenticated), 403 (authenticated but forbidden), and validation/error responses.
- Add tests for cross-user and cross-project access attempts.

**Acceptance criteria:**
- Missing, invalid, expired, or wrong-type access tokens cannot access protected routes.
- Changing an ID in a request does not grant access to another user's data.
- Role, membership, and CSRF rules are tested.
- Frontend route protection is treated as UX, not the security boundary.

## T9.6 — Integrate cookie-based authentication in the frontend

**Owner:** Frontend  
**Priority:** Required

**Requirement:** Provide registration, login, logout, session restoration, and authenticated navigation without exposing JWT values to browser JavaScript.

**Work:**
- Implement accessible registration and login forms with loading, validation, and safe error states.
- Configure the API client to send cookies (`credentials: "include"` for cross-origin fetch requests) and ensure server CORS settings match.
- Do not use `localStorage`, `sessionStorage`, React state, URL parameters, or JavaScript-readable cookies to store access/refresh token values.
- On application startup, request the current-user/session endpoint; do not infer authentication solely from a cookie existing in frontend JavaScript.
- When an API request receives an access-token expiry/401 response, attempt at most one coordinated refresh and retry for eligible requests. Avoid infinite refresh loops and duplicate refresh races.
- If refresh fails, clear client-side user state and redirect to login; do not claim the user is authenticated.
- Ensure logout calls the backend revocation endpoint before updating the UI, then clear local user/session state.
- Implement route and navigation guards appropriate to Next.js, while keeping backend authorization authoritative.
- For server-side rendering or Server Components, forward cookies only through a deliberate, documented server-side API strategy; never expose token values to client components.
- Add CSRF handling required by the backend contract.

**Acceptance criteria:**
- Login, refresh, page reload/session restoration, and logout work end to end.
- Token values are inaccessible to frontend JavaScript and absent from client logs.
- A single expired access token does not cause an infinite retry loop.
- Unauthorized users cannot access protected data, even if they bypass frontend route guards.
- CORS, cookie, CSRF, and production HTTPS settings are documented.

## T9.7 — Complete authentication security verification

**Owner:** Frontend + Backend + Database  
**Priority:** Required before public deployment

**Requirement:** Verify the complete authentication lifecycle and common failure/attack cases.

**Work:**
- Add backend unit and integration tests for JWT claims, expiry, signature failure, token-type separation, cookie attributes, refresh rotation, revocation, and replay detection.
- Add frontend tests for login states, startup session restoration, refresh failure, logout, and protected navigation.
- Add end-to-end tests for login → protected request → access expiry → refresh → logout.
- Verify cookies are `HttpOnly`, production `Secure`, and configured with the intended `SameSite`, path, and domain values.
- Verify credentialed CORS rejects untrusted origins and CSRF protection rejects unsafe cross-site requests.
- Confirm secrets and tokens are redacted from logs and errors.
- Document how to rotate JWT signing keys and what happens to existing sessions.

**Acceptance criteria:**
- Authentication lifecycle tests pass.
- Untrusted origins and CSRF attempts are rejected.
- Access and refresh tokens are never exposed in response bodies, URLs, or logs.
- Production secrets and cookie settings are checked before release.


# Phase 10 — Database engineering

## T10.1 — Add indexes based on query patterns

**Owner:** Database + Backend  
**Priority:** Required before performance tuning

**Requirement:** Improve common query paths without creating speculative indexes.

**Work:**
- Identify common project and task list queries.
- Review filter and sort combinations.
- Use `EXPLAIN` and, on suitable test data, `EXPLAIN ANALYZE`.
- Add indexes through new migrations.
- Consider write overhead and index size.

**Acceptance criteria:**
- Each new index has a documented query pattern.
- Relevant query plans or benchmark results are recorded.
- Existing migration history remains intact.

## T10.2 — Implement pagination and filtering efficiently

**Owner:** Backend + Database  
**Priority:** Required

**Requirement:** Prevent unbounded result sets and provide stable task lists.

**Work:**
- Start with bounded offset pagination if adequate.
- Use deterministic sorting with a unique tie-breaker.
- Consider keyset pagination for deep scrolling.
- Validate page size and filter/sort fields.
- Add suitable indexes only after observing query patterns.

**Acceptance criteria:**
- Requests cannot retrieve unlimited rows.
- Pagination is stable for a consistent dataset.
- Filters return correct results.
- Large-page and invalid-parameter cases are tested.

## T10.3 — Add one meaningful stored procedure exercise

**Owner:** Database, reviewed by Backend  
**Priority:** Learning objective; not required for basic CRUD

**Requirement:** Learn when stored procedures are useful without moving all application logic into SQL.

**Work:**
- Select a clear use case, such as a project task-summary/reporting operation.
- Compare a plain SQL query, a database function, a stored procedure, and backend service logic where relevant.
- Choose the correct object type for PostgreSQL semantics.
- Add the definition through a versioned migration.
- Document parameters, permissions, side effects, and transaction assumptions.
- Add tests against a disposable PostgreSQL database.
- Do not use a stored procedure merely to wrap a simple query without a reason.

**Acceptance criteria:**
- The use case and trade-offs are documented.
- The SQL object is version-controlled and tested.
- Backend and database responsibilities remain clear.

## T10.4 — Learn query and ORM behavior

**Owner:** Backend + Database  
**Priority:** Required learning stage

**Requirement:** Understand how JPA/Hibernate queries translate to SQL and identify inefficient data access.

**Work:**
- Inspect generated SQL in development.
- Demonstrate an N+1 query case in a controlled test.
- Fix it with projections, fetch joins, entity graphs, or another justified approach.
- Use transactions deliberately.
- Avoid serializing lazy entities directly from controllers.

**Acceptance criteria:**
- A before/after example is documented.
- Correctness is preserved.
- Query count or measured behavior improves for the selected case.

---

# Phase 11 — Validation, error handling, and maintainability

## T11.1 — Standardize backend errors

**Owner:** Backend  
**Priority:** Required

**Requirement:** Return predictable API errors without exposing internal implementation details.

**Work:**
- Add centralized exception handling.
- Define stable error fields such as code, message, and field validation details.
- Map validation, not-found, conflict, unauthorized, and forbidden cases.
- Add correlation/request IDs if appropriate.
- Keep stack traces in protected server logs, not client responses.

**Acceptance criteria:**
- Error response shape is documented.
- Tests verify status codes and payloads.
- Sensitive internal details are not exposed.

## T11.2 — Improve frontend resilience

**Owner:** Frontend  
**Priority:** Required

**Requirement:** Make common API failures understandable to users.

**Work:**
- Add route-level error handling and loading UI.
- Handle network failures and server validation responses.
- Prevent accidental duplicate submissions where needed.
- Preserve user input when practical after a recoverable failure.
- Avoid swallowing errors silently.

**Acceptance criteria:**
- Users see actionable feedback for common failure cases.
- No uncaught errors occur in tested flows.
- Forms remain accessible.

---

# Phase 12 — Automated testing strategy

## T12.1 — Backend unit tests

**Owner:** Backend  
**Priority:** Required

**Requirement:** Test business rules independently from HTTP and database infrastructure.

**Work:**
- Test task validation and status transitions.
- Test project and membership rules.
- Test service behavior for expected success and failure paths.
- Use mocks only where they provide a useful boundary.

**Acceptance criteria:**
- Core domain rules have focused tests.
- Tests are deterministic.
- Test names describe observable behavior.

## T12.2 — Backend integration tests

**Owner:** Backend + Database  
**Priority:** Required

**Requirement:** Verify Spring wiring, REST behavior, persistence, and migration compatibility.

**Work:**
- Test API validation and error responses.
- Test persistence against PostgreSQL where practical.
- Use Testcontainers if available and suitable.
- Verify migrations from a clean database.
- Test transaction rollback and uniqueness constraints.

**Acceptance criteria:**
- Integration tests are repeatable.
- They do not depend on a developer's production or shared database.
- Environment prerequisites are documented.

## T12.3 — Frontend tests

**Owner:** Frontend  
**Priority:** Required for critical flows

**Requirement:** Verify important user-visible behavior rather than component internals alone.

**Work:**
- Test forms, loading/error states, task status changes, and key navigation.
- Reuse the existing test setup or agree on one before adding new dependencies.
- Add end-to-end tests for critical workflows if the project adopts a suitable browser test tool.

**Acceptance criteria:**
- Critical user interactions are covered.
- Tests do not depend on live production services.
- Test setup and commands are documented.

## T12.4 — End-to-end acceptance flow

**Owner:** Frontend + Backend + Database  
**Priority:** Required

**Requirement:** Prove the system works across component boundaries.

**Minimum flow:**
1. Register and log in.
2. Create a project.
3. Add or verify a project member.
4. Create a task.
5. Update the task status.
6. Filter or search tasks.
7. Verify persistence after reloading.
8. Confirm unauthorized users cannot access protected resources.

**Acceptance criteria:**
- The flow works against a test/local environment.
- Failures are diagnosable.
- The test does not use production credentials or data.

---

# Phase 13 — Optional caching and asynchronous work

These tasks are deliberately postponed until the core app and its tests work.

## T13.1 — Measure before caching

**Owner:** Backend + Database  
**Priority:** Optional advanced learning

**Requirement:** Establish a performance baseline before introducing Redis.

**Work:**
- Measure selected dashboard or project-summary queries.
- Identify whether the bottleneck is database access, application work, or network latency.
- Record baseline latency and query behavior.
- Define acceptable freshness for the chosen data.

**Acceptance criteria:**
- There is evidence that caching is useful for the selected case.
- Cache requirements and invalidation rules are documented.

## T13.2 — Add Redis caching

**Owner:** Backend  
**Priority:** Optional advanced learning

**Requirement:** Cache a specific read-heavy result while keeping PostgreSQL authoritative.

**Work:**
- Add Redis only after dependency and configuration review.
- Define cache keys, TTLs, invalidation, and error behavior.
- Prevent personalized data from leaking between users.
- Handle Redis outages gracefully.
- Measure before and after behavior.

**Acceptance criteria:**
- Cache invalidation is tested.
- Redis failure does not silently corrupt durable data.
- Performance results are documented.

## T13.3 — Design notification events

**Owner:** Backend + Database  
**Priority:** Optional advanced learning

**Requirement:** Decouple slow notification delivery from user-facing task operations.

**Work:**
- Define event contracts such as task created or assigned.
- Decide what data belongs in an event and avoid sensitive payloads.
- Consider a transactional outbox to prevent database-commit/event-publish inconsistency.
- Keep notification delivery separate from the main task request.

**Acceptance criteria:**
- Event schema and failure behavior are documented.
- Duplicate delivery is considered.
- No messaging infrastructure is introduced until the design is clear.

## T13.4 — Add Kafka notification processing

**Owner:** Backend  
**Priority:** Optional advanced learning

**Requirement:** Process notification events asynchronously.

**Work:**
- Configure a topic and producer/consumer only when ready.
- Add bounded retries and backoff.
- Design idempotent event handling.
- Handle poison messages and dead-letter processing.
- Track consumer lag and failures.
- Test the case where a consumer repeats work after a crash.

**Acceptance criteria:**
- Duplicate events do not create duplicate effects.
- Retry behavior is bounded and documented.
- The core task API remains usable if notification delivery is delayed.

---

# Phase 14 — Operational readiness

## T14.1 — Add health and operational visibility

**Owner:** Backend  
**Priority:** Required before deployment

**Requirement:** Make application health and failures observable without exposing operational details publicly.

**Work:**
- Configure Spring Boot Actuator as appropriate.
- Define liveness/readiness behavior.
- Add structured logs and correlation IDs.
- Avoid logging secrets and sensitive personal data.
- Protect health, metrics, and diagnostic endpoints.

**Acceptance criteria:**
- Health checks reflect the intended dependencies.
- Logs help diagnose failures.
- Sensitive operational endpoints are protected.

## T14.2 — Add Docker-based local development

**Owner:** Root + Backend + Database  
**Priority:** Recommended

**Requirement:** Let developers start required local services consistently.

**Work:**
- Maintain the root Docker Compose configuration for local PostgreSQL, Redis, and Kafka.
- Redis and Kafka containers may be started from the beginning; integrate them into application features only in their relevant later tasks.
- Use named volumes where persistence is desired and health checks where appropriate.
- Use named volumes where persistence is desired.
- Keep credentials local and use placeholders in examples.
- Document startup, shutdown, and data reset behavior.
- Never make a destructive reset command the default.

**Acceptance criteria:**
- A new developer can follow the documented setup.
- Services have health checks where appropriate.
- Data persistence and reset behavior are clear.

## T14.3 — Add CI checks

**Owner:** Root + Frontend + Backend  
**Priority:** Recommended

**Requirement:** Catch build and test failures before changes are merged.

**Work:**
- Run frontend lint/build checks.
- Run backend Maven tests and build.
- Validate SQL naming and migration conventions with a safe check.
- Run integration tests only where infrastructure is available and appropriately isolated.
- Cache dependencies only where safe.
- Do not expose secrets in CI logs.

**Acceptance criteria:**
- CI fails when required checks fail.
- Local commands are documented and match CI.
- No deployment occurs as an unintended side effect of a test workflow.

## T14.4 — Prepare deployment configuration

**Owner:** Root + Frontend + Backend + Database  
**Priority:** Final release stage

**Requirement:** Document deployment without binding the application prematurely to a single provider.

**Work:**
- Separate development and production configuration.
- Define frontend/backend URLs, CORS, database connectivity, and secret management.
- Use managed PostgreSQL or another reviewed database deployment approach.
- Plan migration execution and recovery.
- Define backups, restore testing, logging, and monitoring.
- Add provider-specific deployment files only when a target environment is selected.

**Acceptance criteria:**
- Deployment steps are reproducible.
- Secrets are supplied externally.
- Database backup and recovery expectations are documented.
- A clean environment can be deployed using the documented procedure.

---

# Phase 15 — Final release and learning review

## T15.1 — Security and quality review

**Owner:** All components  
**Priority:** Required

**Requirement:** Review the application as an integrated system.

**Checklist:**
- Authentication and authorization are enforced server-side.
- Users cannot access other projects by changing IDs.
- Inputs are validated on the backend.
- No secrets are committed or logged.
- API errors do not expose stack traces.
- Database constraints protect important invariants.
- Migrations work from a clean database.
- Pagination and filters are bounded.
- Accessibility and responsive layouts are checked.
- Dependencies and versions are reviewed.
- Tests pass in the intended environment.

**Acceptance criteria:**
- Findings are documented and critical issues are fixed before release.
- Unverified areas are explicitly listed.

## T15.2 — Documentation and architecture review

**Owner:** All components  
**Priority:** Required

**Requirement:** Make the project understandable to another developer and useful as a portfolio/learning project.

**Work:**
- Document architecture and data flow.
- Document local setup and common commands.
- Document API endpoints and database migrations.
- Record important decisions and trade-offs.
- Document known limitations and future work.
- Explain why the backend remains a modular monolith and which infrastructure is optional.

**Acceptance criteria:**
- A new developer can set up the project from the README.
- Key technical choices are explained.
- The repository contains no misleading claims about features or tests.

---

## 5. Recommended implementation order

Use this order to avoid building infrastructure before the product works:

1. Repository inspection and shared contract
2. Frontend, backend, and database boilerplate verification
3. Relational model and first migration
4. PostgreSQL connection and migration execution
5. Backend task/project domain
6. Project and task CRUD APIs
7. Frontend application shell and core pages
8. Typed API integration
9. JWT authentication design, secure cookie contract, and CSRF/CORS decisions
10. Registration/login with short-lived access JWT and long-lived refresh token
11. Refresh rotation, replay detection, logout, and revocation
12. Frontend cookie-based authentication and session restoration
13. Project-level authorization
14. Comments and memberships
15. API documentation and consistent errors
16. Database indexes and query optimization
17. Unit, integration, and end-to-end tests (including auth lifecycle)
18. Stored procedure learning exercise
19. Health checks, logs, Docker Compose, and CI
20. Optional Redis caching (local Redis container is already available)
21. Optional Kafka notifications and outbox (local Kafka container is already available)
22. Deployment, security review, and final documentation

Security must be in place before exposing real user data publicly; the sequence above does not authorize a public insecure deployment.

---

## 6. Definition of done for every task

A task is complete only when all relevant points are true:

- [ ] Requirement is implemented as described.
- [ ] Changes are limited to the assigned component unless cross-component work is necessary.
- [ ] Existing code and user changes are preserved.
- [ ] Input validation and error behavior are considered.
- [ ] Tests are added or updated where appropriate.
- [ ] Relevant lint, build, or database checks have actually run.
- [ ] Documentation is updated where behavior or setup changed.
- [ ] No real secrets are introduced.
- [ ] Diff is reviewed for unrelated changes.
- [ ] Actual validation results and remaining risks are reported.

---

## 7. Suggested task tracking template

Copy this template for each implementation task:

```markdown
## Task ID: T?.?

**Title:**
**Owner component:** Frontend / Backend / Database / Root
**Priority:** Required / Recommended / Optional
**Depends on:**

### Requirement
What must the system do, and why?

### Scope
What files or behaviors may change?

### Out of scope
What must not be implemented in this task?

### Implementation notes
Relevant architecture, data contracts, validation, security, and edge cases.

### Acceptance criteria
- [ ] Observable requirement 1
- [ ] Observable requirement 2
- [ ] Relevant tests pass
- [ ] Documentation updated if needed

### Verification
Commands/checks actually run and results.

### Notes / risks
Assumptions, unresolved questions, or follow-up tasks.
```

---

## 8. Final target

At the end of the required phases, TaskFlow should be a coherent, documented, secure, tested full-stack application with:

- A responsive Next.js frontend
- A Java 21/Spring Boot modular monolith
- PostgreSQL with version-controlled migrations
- JWT authentication with a short-lived access token and long-lived, rotating refresh token stored in secure HttpOnly cookies
- Authentication lifecycle support: refresh, logout, revocation, CSRF/CORS protections, and project-level authorization
- Project and task workflows
- Typed, documented REST APIs
- Repeatable local development and automated checks
- Docker Compose development services for PostgreSQL, Redis (`localhost:6379`), and Kafka (`localhost:9092`)
- A clear test and deployment story

Redis and Kafka run locally in Docker as development services. Their application-level use (caching, rate limiting, event producers/consumers, and notifications) remains a later learning stage, after the core application works. Stored procedure exercises and other advanced topics are also staged learning extensions.
