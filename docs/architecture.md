# TaskFlow Architecture

**Date:** 2026-10-21

---

## System overview

TaskFlow is a **modular monolith** — one deployable backend process, one Next.js frontend, one PostgreSQL database, with optional Redis and Kafka. The architecture was chosen to match the scale of a learning project: simple to operate, straightforward to reason about, and easy to evolve without premature infrastructure complexity.

```
Browser
  │
  │  HTTP (REST + JSON, /api/v1)
  ▼
Next.js frontend          (port 3000)
  │
  │  fetch() calls
  ▼
Spring Boot backend        (port 8080)
  │         │         │
  │         │         └── Kafka producer (outbox relay)
  │         │
  │         └── Redis (optional cache)
  │
  └── PostgreSQL            (port 5432)
```

---

## Request data flow

A typical authenticated request (e.g., listing tasks):

```
1. Browser sends GET /api/v1/projects/1/tasks with access-token cookie
2. OriginCheckFilter → rejects cross-origin requests not in CORS allowlist
3. JwtAuthenticationFilter → validates JWT, sets AuthenticatedUser in SecurityContext
4. Spring Security → checks route requires authentication
5. TaskController → parses request params, calls TaskService
6. TaskService → checks project existence and caller membership (requireMember)
7. JdbcTaskRepository → executes parameterised SQL via JdbcClient
8. PostgreSQL → returns rows; Flyway-managed schema enforces constraints
9. TaskController → serialises PageResponse<TaskResponse> as JSON
10. Browser receives response
```

---

## Component responsibilities

| Component | Owns | Must not do |
|-----------|------|-------------|
| `frontend/` | Pages, UI, RTK Query API layer | Connect directly to PostgreSQL; implement auth/authz logic |
| `backend/` | REST API, business rules, authz, persistence | Contain UI logic; assume the caller is trusted |
| `database/` | Versioned SQL migrations, schema docs, procedures | Duplicate business logic without a documented reason |

---

## Key architectural decisions

### Modular monolith over microservices

**Decision:** The backend is a single Spring Boot application. Service classes (`ProjectService`, `TaskService`, etc.) are separated by package but share a JVM and a database connection pool.

**Why:** The project's domain (tasks, projects, members, comments) is small and cohesive. Splitting into microservices would add network hops, distributed transaction complexity, and operational overhead with no benefit at this scale. If the project needed to scale a specific domain independently, the package boundaries make that extraction tractable.

### JWT + HttpOnly cookie authentication

**Decision:** Access tokens are JWT-signed with HS256 and stored in HttpOnly, SameSite=Strict cookies. Refresh tokens are stored as hashed values in PostgreSQL.

**Why:** HttpOnly cookies are not readable by JavaScript, so XSS cannot steal them. SameSite=Strict blocks CSRF from third-party origins. This combination removes the need for a JS-accessible CSRF token. See `docs/auth-design-adr.md` for the full decision record.

### Transactional outbox for Kafka events

**Decision:** Rather than publishing Kafka events directly from service methods, events are written to an `outbox_events` table in the same database transaction, then forwarded to Kafka by a scheduled relay.

**Why:** A direct publish inside a transaction risks either losing events (broker down at commit time) or publishing phantom events (broker succeeds but DB rolls back). The outbox pattern makes event delivery exactly-once at the database level and at-least-once at the broker level. Idempotency keys (`event_id` UUID) let consumers deduplicate.

### Optional Redis and Kafka

**Decision:** The application starts and serves traffic correctly without Redis and without Kafka.

- `CacheConfig` falls back to `NoOpCacheManager` when Redis is unreachable. All reads fall through to the database.
- `OutboxRelayService` checks `Optional<KafkaTemplate>` and skips publishing when Kafka is absent. Outbox rows accumulate but no runtime error is thrown.

**Why:** This makes local development and CI easier (no mandatory external services) while the infrastructure is still there when needed.

### Spring Cache with `allEntries=true` eviction

**Decision:** `@CacheEvict(allEntries=true)` clears all per-user project-list pages when a project is mutated.

**Why:** Per-user cache keys include `{callerId}:{page}:{size}`. Evicting by user ID alone would miss other pages. `allEntries=true` clears the whole cache namespace, which is safe because:
1. The cache TTL is only 30 seconds.
2. Membership changes are infrequent.

---

## Optional infrastructure summary

| Service | Required to start? | Impact when absent |
|---------|--------------------|--------------------|
| Redis | No | Cache disabled; all reads hit PostgreSQL |
| Kafka | No | Notification relay skips; outbox rows accumulate |
| PostgreSQL | Yes | Application cannot start without a datasource |

---

## API endpoints summary

All endpoints are under `/api/v1`. See Swagger UI at `http://localhost:8080/swagger-ui.html` for full request/response schemas.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | None | Register a new user |
| POST | `/auth/login` | None | Log in, receive access-token cookie |
| POST | `/auth/logout` | Required | Invalidate session |
| POST | `/auth/refresh` | Cookie | Issue new access token from refresh token |
| GET | `/auth/me` | Required | Return current user profile |
| GET/POST | `/projects` | Required | List / create projects |
| GET/PATCH/DELETE | `/projects/{id}` | Required | Get / update / delete project |
| GET/POST | `/projects/{id}/members` | Required | List / add members |
| PATCH/DELETE | `/projects/{id}/members/{userId}` | Required | Update role / remove member |
| GET/POST | `/projects/{id}/tasks` | Required | List / create tasks |
| GET/PATCH/DELETE | `/tasks/{id}` | Required | Get / update / delete task |
| PATCH | `/tasks/{id}/status` | Required | Transition task status |
| GET/POST | `/tasks/{id}/comments` | Required | List / add comments |
| PATCH/DELETE | `/comments/{id}` | Required | Edit / delete comment |

---

## Database migrations

Migration files live in `database/migrations/` and follow Flyway naming: `V<n>__<snake_case_description>.sql`.

| Migration | Description |
|-----------|-------------|
| V1 | Initial schema: users, projects, project_members, tasks |
| V2 | Add comments table |
| V3 | Add auth fields to users (password_hash) |
| V4 | Add refresh_tokens table |
| V5 | Query performance indexes |
| V6–V11 | Stored procedures for CRUD operations |
| V12 | outbox_events table for notification relay |

Flyway runs on every startup; applied migrations are never re-run. See `README.md` for migration rules.

---

## Known limitations and future work

| Area | Limitation | Possible improvement |
|------|------------|---------------------|
| Notification delivery | Only logged; no real email/push | Implement notification preference table and an email sender |
| Single-region | No geographic redundancy | Deploy to multiple regions with a read replica |
| Task attachments | Not supported | Add S3-compatible blob storage |
| Full-text search | SQL ILIKE only | Add `pg_trgm` or Elasticsearch for better search |
| Rate limiting | No per-user rate limiting | Add a rate-limit filter or API gateway |
| Audit log | No history of changes | Add an `audit_log` table or CDC |
| Accessibility | Not audited | Run Lighthouse / axe audit; add ARIA labels to interactive elements |
| Frontend test coverage | Forms and NavBar unit-tested; no E2E in CI | Add Playwright E2E tests to the CI workflow |
