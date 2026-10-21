# Migration Execution Plan

This document describes how SQL migration files in `database/migrations/` are packaged, delivered to Flyway, and executed across environments.

---

## Build Integration

Migration files are not symlinked or referenced by filesystem path. Instead, the Maven build copies them onto the backend's classpath:

```
database/migrations/*.sql
    ──(maven-resources-plugin at process-resources phase)──▶
        backend/target/classes/db/migration/
            available as classpath:db/migration
```

**Why:** Flyway's default classpath scan location is `classpath:db/migration`. Copying files there during the build means every environment (local, CI, production) uses the same packaged artifact — no developer needs to configure a local filesystem path.

The Maven Resources Plugin execution is declared in `backend/pom.xml` under the id `copy-flyway-migrations`. It copies only `*.sql` files (no documentation or config files are packaged).

---

## Flyway Dependency

Flyway (`flyway-core`) and the datasource (`spring-boot-starter-data-jpa`) are **not yet added** to `backend/pom.xml`. They will be introduced in T4.x. Until then:

- The `copy-flyway-migrations` build step runs harmlessly (copies nothing, since no `.sql` files exist yet).
- The commented-out `spring.flyway.*` and `spring.datasource.*` properties in `application.properties` document the intended configuration but are inactive.

---

## When Migrations Run

| Context | When | How |
|---------|------|-----|
| Local development | On every `./mvnw spring-boot:run` startup | Flyway auto-runs when datasource + `flyway-core` are on the classpath |
| Integration tests | On every `@SpringBootTest` startup | Same Flyway auto-run; a test-scoped datasource will be configured in T4.x |
| CI build | On application startup in the CI environment | Credentials supplied via CI secrets |
| Production / staging | On application startup after deploy | Same mechanism; gated by deployment pipeline approval |

---

## Local Developer Setup

A developer starting from a clean checkout:

```bash
# 1. Start the local PostgreSQL instance
docker compose up -d

# 2. Verify it is healthy
docker compose ps

# 3. Configure backend credentials
cd backend
cp .env.example .env   # fill in SPRING_DATASOURCE_PASSWORD

# 4. Start the backend — Flyway runs automatically on startup (once T4.x is done)
./mvnw spring-boot:run   # Windows: mvnw.cmd spring-boot:run
```

The Docker Compose service (`postgres:17.2`) creates the `taskflow` database on first start. Flyway then applies pending migrations in version order.

---

## Destructive Reset

Flyway does not run destructive scripts automatically. To wipe and recreate the local database:

```bash
# WARNING: permanently deletes all local PostgreSQL data.
docker compose down -v
docker compose up -d
```

After a reset, the next application startup re-applies all migrations from V1 on a clean database.

---

## Adding a New Migration

1. Create `database/migrations/V<next>__<description>.sql`.
2. The Maven build picks it up automatically on the next `mvnw package` or `spring-boot:run`.
3. Flyway executes it on the next application startup.
4. Never edit or delete a migration file once it has been applied to a shared environment.

---

## Flyway Configuration Reference

When Flyway is activated in T4.x, `backend/src/main/resources/application.properties` will include:

```properties
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=false
```

- `locations` — points Flyway to the directory populated by the Maven build step.
- `baseline-on-migrate=false` — all migrations apply from V1 on a clean database; no implicit baseline is created.
