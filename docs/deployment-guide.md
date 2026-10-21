# Deployment Guide

**Status:** Pre-deployment documentation — no target provider committed yet  
**Date:** 2026-10-21

---

## Dev vs Production configuration separation

| Concern | Local dev | Production |
|---------|-----------|------------|
| Secrets | `.env` file (gitignored) | Environment injection via platform secret store |
| Database | Docker Compose Postgres (`localhost:5432`) | Managed PostgreSQL (e.g. Supabase, RDS, Cloud SQL) |
| Redis | Docker Compose Redis (`localhost:6379`) | Managed Redis (e.g. Upstash, ElastiCache) |
| Kafka | Docker Compose Kafka (`localhost:9092`) | Managed Kafka (e.g. Confluent Cloud, Amazon MSK) |
| CORS | `CORS_ALLOWED_ORIGINS=http://localhost:3000` | Set to the production frontend origin |
| Cookie | `COOKIE_SECURE=false` | `COOKIE_SECURE=true` (Secure flag on access-token cookie) |
| Log format | Human-readable | Structured JSON via `STRUCTURED_LOGGING=logstash` |

---

## Required secrets (backend)

All values must be supplied via environment variables. **Never commit real values.**

| Variable | Purpose | Notes |
|----------|---------|-------|
| `SPRING_DATASOURCE_URL` | JDBC connection string | `jdbc:postgresql://<host>:<port>/<db>` |
| `SPRING_DATASOURCE_USERNAME` | DB user | Least-privilege user; no DDL rights in prod |
| `SPRING_DATASOURCE_PASSWORD` | DB password | Rotate periodically |
| `JWT_SECRET` | HS256 signing key | Minimum 32 random bytes (256-bit); rotate on breach |
| `REDIS_HOST` / `REDIS_PORT` | Cache backing store | App falls back to no-op cache if unreachable |
| `KAFKA_BOOTSTRAP_SERVERS` | Notification event bus | App publishes to outbox; relay skips if unavailable |
| `CORS_ALLOWED_ORIGINS` | Frontend origin allowlist | Comma-separated; set to prod frontend URL |
| `COOKIE_SECURE` | `true` in production | Enforces HTTPS-only for access-token cookie |

---

## Database migration execution

1. **Before first deployment:** run `./mvnw flyway:migrate -pl backend` or let the Spring Boot app run Flyway on startup (default).
2. **Subsequent deploys:** Flyway applies new `V*` migrations automatically on startup; existing migrations are never re-run.
3. **Rollback:** Flyway Community does not support rollback. Write a new forward migration to undo a change.
4. **Zero-downtime:** for non-destructive schema changes (add column, add index CONCURRENTLY), deploy the new migration and then the new app version. For destructive changes, use a two-phase migration (add column → migrate data → remove old column in a separate migration).

### Backup expectations

- **Frequency:** daily logical dump (`pg_dump`) plus continuous WAL archiving for point-in-time recovery.
- **Retention:** 30-day dump history; WAL retention aligned to the provider's SLA.
- **Restore test:** monthly restore drill to a staging environment; verify Flyway migration state and row counts.
- **Provider note:** managed PostgreSQL providers (RDS, Supabase) automate WAL archiving; configure automated snapshots and set a retention window.

---

## Deployment procedure (provider-agnostic)

### Backend

1. Build the JAR: `./mvnw package -DskipTests -pl backend`
2. Set all required environment variables in the target environment.
3. Run `java -jar backend/target/*.jar` (or via container: `docker build -t taskflow-backend backend/ && docker run ...`).
4. Flyway runs on startup; confirm `/actuator/health` returns `{"status":"UP"}` within 30 s.
5. Verify `/api/v1/auth/me` returns 401 (filter chain active).

### Frontend

1. Build: `cd frontend && npm ci && npm run build`
2. Serve: `npm start` (Next.js built-in server) or export for static hosting (`next export` if no SSR is used).
3. Set `NEXT_PUBLIC_API_BASE_URL=https://<backend-hostname>`.
4. Verify the login page loads and authentication completes end-to-end.

### Provider-specific files

Add `Dockerfile`, `render.yaml`, `fly.toml`, Kubernetes manifests, or Terraform modules **only when a target environment is selected**. Keep them in the relevant component directory (`backend/`, `frontend/`, `infra/`).

---

## Monitoring and logging

- **Health endpoint:** `GET /actuator/health` — returns `{"status":"UP","components":{"db":{...},"redis":{...}}}`.
- **Correlation IDs:** every request logs a UUID in `correlationId` MDC field; echoed in `X-Correlation-ID` response header.
- **Structured logs:** set `STRUCTURED_LOGGING=logstash` to emit JSON lines compatible with Logstash/Elastic.
- **Kafka consumer lag:** monitor `taskflow-notifications` consumer group lag via Kafka's consumer group commands or a managed dashboard.
- **Outbox dead letters:** rows in `outbox_events` with `attempts >= 5` and `delivered = false` are dead-lettered in-place; alert on this count.

---

## CORS

`CORS_ALLOWED_ORIGINS` is a comma-separated list of allowed origins. In production, set it to exactly the frontend origin (e.g. `https://app.example.com`). Wildcard (`*`) must not be used because the API sets cookies.
