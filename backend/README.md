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

## API

All endpoints are versioned under `/api/v1`. See the root [README](../README.md) for the shared API contract and error response shape.
