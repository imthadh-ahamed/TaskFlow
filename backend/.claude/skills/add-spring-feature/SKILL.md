---
name: add-spring-feature
description: Add a small, maintainable feature to the TaskFlow Spring Boot backend using the repository's current conventions.
---

# Add a Spring Boot feature

1. Inspect `pom.xml`, the application package, existing controllers/services, configuration, and tests.
2. Confirm the feature's acceptance criteria and affected API contracts.
3. Follow the repository's existing package structure. If none exists yet, propose a feature-oriented structure such as `task/api`, `task/application`, `task/domain`, and `task/infrastructure`.
4. Add request validation and DTOs appropriate to the current dependencies.
5. Keep business logic outside controllers and use constructor injection.
6. Add tests for normal behavior and meaningful failure paths.
7. Update API documentation or README content if the feature changes how the app is used.
8. Run relevant tests with the Maven Wrapper and report exact outcomes.
9. Do not add PostgreSQL, JPA, Flyway, Spring Security, Redis, Kafka, or other dependencies unless the feature requires them and the user approves the change.
