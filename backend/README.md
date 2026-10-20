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

Copy `.env.example` to `.env` and fill in values (added in T2.2):

```bash
cp .env.example .env
```

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
