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
| `SPRING_DATASOURCE_URL` | T3.x (persistence) | — | `jdbc:postgresql://localhost:5432/taskflow` locally |
| `SPRING_DATASOURCE_USERNAME` | T3.x (persistence) | — | Match the Docker Compose user |
| `SPRING_DATASOURCE_PASSWORD` | T3.x (persistence) | — | Never commit |
| `JWT_SECRET` | Auth phase | — | Minimum 32 random characters |

### Logging

- Package `com.example.backend` logs at the level set by `LOG_LEVEL` (default `INFO`).
- Spring framework logs are suppressed to `WARN` to reduce noise.
- Set `LOG_LEVEL=DEBUG` in `.env` when debugging locally.

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
