# TaskFlow

A full-stack task and project management application built as a learning project. Users can register, create projects, manage project members, create and assign tasks, move tasks through statuses, and comment on work.

**Architecture:** modular monolith — one deployable Spring Boot backend, one Next.js frontend, one PostgreSQL database.

---

## Repository structure

```
TaskFlow/
├── frontend/          # Next.js 16, React 19, TypeScript, Tailwind CSS
├── backend/           # Java 21, Spring Boot 4, Spring MVC
├── database/          # PostgreSQL SQL assets, Flyway migrations, docs
├── docker-compose.yml # Local development services (PostgreSQL, Redis, Kafka)
├── TaskFlow_Task.md   # Zero-to-end implementation plan
└── README.md          # This file — shared contract and setup guide
```

---

## Component responsibilities

| Component | Owns | Must not do |
|-----------|------|-------------|
| `frontend/` | Pages, UI, client-side interactions, typed API integration | Connect directly to PostgreSQL; implement authoritative access control |
| `backend/` | REST API, business rules, authentication/authorization, persistence | Contain UI concerns; assume frontend visibility is security |
| `database/` | Versioned SQL migrations, schema docs, seed data, procedures/functions/views | Become an undocumented second source of business logic |

**Source of truth:** PostgreSQL stores durable data. The backend enforces business rules and authorization. The frontend presents data and calls the backend over HTTP only.

---

## Local URLs

| Service | URL |
|---------|-----|
| Frontend dev server | `http://localhost:3000` |
| Backend API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI spec (JSON) | `http://localhost:8080/v3/api-docs` |
| OpenAPI spec (YAML) | `http://localhost:8080/v3/api-docs.yaml` |
| PostgreSQL | `localhost:5432` (database `taskflow`) |
| Redis | `localhost:6379` |
| Kafka | `localhost:9092` |

---

## API conventions

- All REST endpoints are versioned under `/api/v1`.
- Authentication endpoints live under `/api/v1/auth`.
- Request and response bodies use `application/json`.
- List endpoints return paginated results; never unbounded collections.

### Error response shape

Every error response uses this JSON structure:

```json
{
  "status": 400,
  "error": "Validation failed",
  "code": "VALIDATION_ERROR",
  "message": "Human-readable description of what went wrong",
  "details": [
    { "field": "title", "message": "must not be blank" }
  ],
  "timestamp": "2026-10-21T10:00:00Z",
  "path": "/api/v1/projects"
}
```

- `status` — HTTP status code (integer).
- `error` — Short HTTP status reason phrase.
- `code` — Machine-readable error code; stable across releases.
- `message` — Human-readable description; do not parse programmatically.
- `details` — Optional array of field-level validation errors.
- `timestamp` — ISO 8601 UTC timestamp.
- `path` — Request URI.

Internal stack traces, SQL errors, and implementation details must never appear in error responses.

---

## Database migrations

**Owner:** `database/` component.

Migration files live in `database/migrations/` and follow the Flyway naming convention:

```
V1__initial_schema.sql
V2__create_indexes.sql
```

**Execution:** The Spring Boot backend invokes Flyway automatically on startup. Flyway reads migrations from `classpath:db/migration`. During the build, the Maven Resources Plugin copies `database/migrations/*.sql` into the backend's classpath at `db/migration/` — no developer-specific filesystem path is required. See `database/docs/migration-execution.md` for the full plan.

Migration rules:
- Migration files are immutable once applied to a shared environment.
- New migrations must be added; existing ones must not be edited in place.
- Schema changes must be reviewed before merging.

---

## Environment variables and secrets

No secrets are committed to Git. Each component has a `.env.example` file listing the required variables with placeholder values. Copy it to `.env` locally and fill in real values.

| Component | Example file | Key variables |
|-----------|-------------|---------------|
| `frontend/` | `frontend/.env.example` | `NEXT_PUBLIC_API_BASE_URL` |
| `database/` | `database/.env.example` | `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` |
| `backend/` | `backend/.env.example` (added in T2.2) | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET` |

Rules:
- Variables prefixed with `NEXT_PUBLIC_` are exposed to browser bundles; never put secrets in them.
- Backend secrets (database passwords, JWT signing keys) must come from environment variables or a secret manager; never from committed configuration files.
- CI/CD environments must supply secrets through their own secret stores.

---

## Local development setup

### Prerequisites

- Java 21
- Node.js 18+ and npm
- Docker and Docker Compose (for PostgreSQL, Redis, Kafka)

### Docker services

`docker-compose.yml` starts three local development services:

| Service | Image | Host port | Container-network address |
|---------|-------|-----------|--------------------------|
| PostgreSQL | `postgres:17.2` | `localhost:5432` | `postgres:5432` |
| Redis | `redis:7.4.1` | `localhost:6379` | `redis:6379` |
| Kafka | `apache/kafka:4.2.2` | `localhost:9092` (host clients) | `kafka:29092` (Docker clients) |

**Kafka listeners:**
- Host applications (Spring Boot running locally) connect to `localhost:9092`.
- Containers running inside the same Compose network use `kafka:29092`.

**Environment variables:** Copy `.env.example` to `.env` and set `POSTGRES_PASSWORD`.

```bash
cp .env.example .env   # then edit .env — set POSTGRES_PASSWORD
```

#### Start and verify

```bash
docker compose up -d      # start all services in the background
docker compose ps         # show running status and health
```

#### View logs

```bash
docker compose logs -f postgres   # follow PostgreSQL logs
docker compose logs -f redis      # follow Redis logs
docker compose logs -f kafka      # follow Kafka logs
docker compose logs -f            # follow all services
```

#### Stop services (data is preserved)

```bash
docker compose stop       # stop containers; volumes kept
docker compose start      # restart stopped containers
```

#### Shut down and remove containers (data is preserved)

```bash
docker compose down       # stop and remove containers; named volumes kept
```

#### ⚠️ Destructive reset — deletes all local data

```bash
# WARNING: this permanently deletes all PostgreSQL, Redis, and Kafka data.
# Only run this when you want a completely clean slate.
docker compose down -v
```

### Frontend

```bash
cd frontend
cp .env.example .env.local   # edit NEXT_PUBLIC_API_BASE_URL if needed
npm install
npm run dev                  # http://localhost:3000
```

### Backend

```bash
cd backend
cp .env.example .env         # fill in database credentials
./mvnw spring-boot:run       # http://localhost:8080
# Windows:
mvnw.cmd spring-boot:run
```

### Available commands

| Component | Command | Purpose |
|-----------|---------|---------|
| Docker | `docker compose up -d` | Start all local services |
| Docker | `docker compose ps` | Show service health |
| Docker | `docker compose logs -f` | Stream all service logs |
| Docker | `docker compose stop` | Stop containers (keep data) |
| Docker | `docker compose down` | Remove containers (keep data) |
| Frontend | `npm run dev` | Development server |
| Frontend | `npm run build` | Production build |
| Frontend | `npm run lint` | ESLint |
| Backend | `./mvnw test` | Run tests |
| Backend | `./mvnw verify` | Build + test |
| Backend | `./mvnw spring-boot:run` | Run application |

---

## Git rules

- One Git repository at the `TaskFlow/` root.
- Do not run `git init` inside component directories.
- Changes spanning multiple components must update their contracts and documentation together.
- No real credentials in any committed file.
