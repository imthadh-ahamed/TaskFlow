# TaskFlow Backend

Spring Boot REST API for the TaskFlow application.

## Stack

| Item | Value |
|------|-------|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Build tool | Maven Wrapper (`./mvnw` / `mvnw.cmd`) |
| Package | `com.example.backend` |

## Setup

```bash
cp .env.example .env   # then edit .env — set real values for any uncommented variables
```

### Configuration

The application reads secrets from environment variables. `application.properties` holds only safe defaults; it never contains passwords or signing keys.

| Variable | Required when | Default | Notes |
|----------|--------------|---------|-------|
| `LOG_LEVEL` | Optional | `INFO` | Set to `DEBUG` locally for verbose output |
| `SPRING_DATASOURCE_URL` | T4.3+ | — | `jdbc:postgresql://localhost:5432/TaskFlow` locally |
| `SPRING_DATASOURCE_USERNAME` | T4.3+ | — | `postgres` locally; `taskflow_app` in shared environments |
| `SPRING_DATASOURCE_PASSWORD` | T4.3+ | — | Never commit |
| `JWT_SECRET` | Auth phase | — | Minimum 32 random characters |

### Logging

- Package `com.example.backend` logs at the level set by `LOG_LEVEL` (default `INFO`).
- Spring framework logs are suppressed to `WARN` to reduce noise.
- Set `LOG_LEVEL=DEBUG` in `.env` when debugging locally.

### Database and migrations

On startup the backend runs any pending Flyway migrations from `classpath:db/migration` (copied from `database/migrations/` at build time by the Maven Resources Plugin). The schema history is tracked in `flyway_schema_history`.

```bash
# Check migration status in the local database
docker exec -it taskflow-postgres psql -U postgres -d TaskFlow \
  -c "SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;"
```

`spring.flyway.validate-on-migrate=true` is always on — the app will refuse to start if a migration file is modified after being applied. To fix a mistake, add a new migration; never edit an applied file.

### Health check

Spring Boot Actuator will be added in a future task. Until then, the application starts on `http://localhost:8080` and readiness can be confirmed by the absence of a startup error.

## Commands

| Command | Purpose |
|---------|---------|
| `./mvnw spring-boot:run` | Start the development server on `http://localhost:8080` |
| `mvnw.cmd spring-boot:run` | Windows equivalent |
| `./mvnw test` | Run unit and integration tests |
| `./mvnw verify` | Build, test, and package |
| `./mvnw package` | Produce the executable JAR in `target/` |
| `./mvnw clean` | Remove build output |

## Package structure

Base package: `com.taskflow`. Code is organised by business capability, not by technical layer.

```
com.taskflow/
├── BackendApplication.java      # Spring Boot entry point
├── auth/                        # Authentication and authorisation
│   ├── api/                     # Controllers — HTTP mapping and request validation only
│   ├── application/             # Use-case orchestration (services)
│   ├── domain/                  # Business rules, entities, value objects
│   └── infrastructure/          # Repositories, external integrations
├── project/                     # Project management capability (same sub-layers as above)
├── membership/                  # Project membership and roles
├── task/                        # Task management capability
├── comment/                     # Task comment capability
├── common/                      # Shared utilities, exception handling, security filters
└── config/                      # Spring configuration beans (@Configuration classes)
```

**Rules:**
- Dependencies flow inward: `infrastructure` → `application` → `domain`. Controllers call application services; they never call repositories directly.
- Keep HTTP concerns (status codes, request/response mapping) in `api/` classes only.
- Use constructor injection throughout; never field injection.
- Do not add a package unless it contains at least one non-empty class.

## API

All endpoints are versioned under `/api/v1`. See the root [README](../README.md) for the shared API contract and error response shape.

### Error responses

All errors are handled centrally by `GlobalExceptionHandler` and return the stable JSON shape defined in the root README. The `X-Request-Id` response header is set on every error response — it echoes the incoming `X-Request-Id` request header when present, or generates a UUID otherwise.

| Exception | HTTP status | `code` field |
|-----------|-------------|--------------|
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` |
| `IllegalArgumentException` | 400 | `INVALID_ARGUMENT` |
| `IllegalStateException` | 400 | `INVALID_TRANSITION` |
| `ResourceNotFoundException` | 404 | `NOT_FOUND` |
| `DataIntegrityViolationException` | 409 | `CONFLICT` |
| `UnauthorizedException` | 401 | `UNAUTHORIZED` |
| `ForbiddenException` | 403 | `FORBIDDEN` |
| Any other `Exception` | 500 | `INTERNAL_ERROR` |

### Repository / stored procedure pattern

All JDBC repositories call PostgreSQL stored procedures via `ConnectionCallback` + `CallableStatement`. Procedures that return a REFCURSOR require the caller service method to be annotated `@Transactional` so the CALL and FETCH share the same connection. See `database/procedures/README.md` for the full inventory.
