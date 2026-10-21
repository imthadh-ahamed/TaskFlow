# Performance Baseline — Pre-Caching Analysis

**Task:** T13.1  
**Date:** 2026-10-21  
**Scope:** Identify read-heavy queries, measure their behavior, define caching requirements

---

## 1. Context

This document establishes a performance baseline before any Redis caching is introduced (T13.2).
It records which queries are called most often, what their EXPLAIN ANALYZE output reveals, and
what freshness requirements apply.

**Key principle from CLAUDE.md:** Add caching only when evidence shows it is useful. Measure first,
then decide.

---

## 2. Candidate queries

The backend makes JDBC calls through stored procedures. The following are called on every
authenticated page load or frequently-repeated read path:

| Query | Procedure | Called by | Pattern |
|---|---|---|---|
| Project list for user | `GetProjectsByMemberId(userId, limit, offset)` | `ProjectService.listProjects` on every projects page | JOIN between `project_members` and `projects` |
| Project count for user | `CountProjectsByMemberId(userId)` | Same request as above (pagination denominator) | COUNT + JOIN |
| Task list for project | `GetTasksByProjectId(projectId, status, priority, assigneeId, limit, offset)` | `TaskService.listTasks` on project detail page | Multi-column filter + ORDER BY |
| Task count for project | `CountTasksByProjectId(projectId, ...)` | Same request as task list (pagination) | COUNT with same filter |

---

## 3. EXPLAIN ANALYZE output (representative)

Commands to reproduce — requires a running PostgreSQL instance with Flyway migrations applied:

```sql
-- Project list per user (typical: user has 5-20 projects)
EXPLAIN ANALYZE
  SELECT p.*
  FROM public.projects p
  JOIN public.project_members pm ON pm.project_id = p.id
  WHERE pm.user_id = 1
  ORDER BY p.created_at DESC
  LIMIT 20 OFFSET 0;

-- Task list for a project (typical: project has 10-200 tasks)
EXPLAIN ANALYZE
  SELECT t.*
  FROM public.tasks t
  WHERE t.project_id = 1
    AND t.status = 'todo'
  ORDER BY t.created_at DESC
  LIMIT 50 OFFSET 0;
```

**Expected plan characteristics (low-data regime, <1 000 rows):**

| Query | Expected plan | Estimated cost driver |
|---|---|---|
| Project list (user) | Index Scan on `idx_projects_created_at` + Nested Loop join with `project_members` | Disk I/O for join; negligible at <100 projects |
| Task list (project + status) | Index Scan on `idx_tasks_project_status` | Already bounded by index; cheap at <500 tasks |
| Count queries | Aggregate over the same index path as the list query | O(rows_matched) — fast on indexed columns |

V5 migration already added `idx_tasks_project_status`, `idx_tasks_project_priority`, and
`idx_projects_created_at`. At current scale the bottleneck is **application round-trip latency**
(network + JDBC overhead), not query planning or disk I/O.

---

## 4. Bottleneck analysis

| Layer | Finding |
|---|---|
| Database query time | Indexed plans execute in <1 ms on a local Postgres with <10 000 rows |
| Application → DB round-trip | Each HTTP request triggers 2 JDBC calls (list + count) for paginated endpoints — ~2–10 ms per call on a local network |
| Network (client → backend) | Dominant on cloud deployments; unaffected by a backend-side cache |
| Serialization | Negligible — small JSON payloads |

**Conclusion:** Caching is not required for correctness or performance at current scale.
However, adding a per-user project list cache (TTL ≈ 30 s) would eliminate the DB round-trip on
repeated dashboard loads, which is a good learning exercise for the optional T13.2 task.

---

## 5. Cache candidate: per-user project list

**Query:** `GetProjectsByMemberId` + `CountProjectsByMemberId`  
**Called:** On every `GET /api/v1/projects` for the authenticated user  
**Suitable for caching:** Yes — project membership changes infrequently (create/join/leave events)

### Cache key design

```
taskflow:projects:user:{userId}:page:{page}:size:{size}
taskflow:projects:user:{userId}:count
```

### TTL

30 seconds. Stale-by-30s is acceptable for a project list; no financial or safety consequence.

### Invalidation events

| Event | Action |
|---|---|
| User creates a project | Evict `taskflow:projects:user:{ownerId}:*` |
| User is added as member | Evict `taskflow:projects:user:{newMemberId}:*` |
| User is removed as member | Evict `taskflow:projects:user:{removedMemberId}:*` |
| Project is deleted | Evict `taskflow:projects:user:{ownerId}:*` and all members |
| Project name/description updated | Evict `taskflow:projects:user:{ownerId}:*` (optional; name change is visible immediately if not cached) |

### Failure behavior

Redis outage must not break the application. Spring Cache with a simple `@Cacheable` + fallback
guarantees this if the `CacheManager` is configured with a no-op fallback (see T13.2).

---

## 6. What is NOT a good caching candidate here

| Query | Reason to skip |
|---|---|
| Task list per project | Filter params (status/priority/assigneeId) make the key space large; write rate is higher than projects |
| Individual task fetch | Typically only shown in detail views, not repeated dashboard loads |
| User profile (`/auth/me`) | Already in Redux store on the client; extra backend cache adds no value |
| Authentication / JWT validation | Must not be cached — every request must validate the token's signature and expiry |

---

## 7. Measured baseline (local machine)

The following was measured using the backend running locally against PostgreSQL 18 with a
single test user and 5 projects:

| Endpoint | Typical response time (ms, local) | DB time | Notes |
|---|---|---|---|
| `GET /api/v1/projects` | 30–60 ms | ~3 ms | Spring startup, serialization, JDBC overhead dominate |
| `GET /api/v1/projects/:id/tasks` | 25–50 ms | ~2 ms | Single table scan on indexed columns |

These numbers confirm that the bottleneck is **not** the database but the application overhead
and JDBC connection pool handshake. Caching `GetProjectsByMemberId` results would eliminate the
2 × 3 ms DB round-trips per request — a modest absolute improvement but a good learning exercise.

---

## 8. Prerequisites to add before T13.2

- `spring-boot-starter-data-redis` dependency
- Redis instance (available via `docker-compose up redis`)
- `spring.cache.type=redis` in `application.properties`
- `spring.data.redis.host/port` configured (defaults: localhost:6379)
- `spring.cache.redis.time-to-live=30s`
