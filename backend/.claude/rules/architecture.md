# Backend architecture rules

- Keep the backend a modular monolith unless there is a clear, documented reason to split it.
- Organize code by business capability as the application grows (for example, `task`, `project`, `auth`), not only by technical layer.
- Keep dependencies flowing toward business rules. Domain/application code should not depend on web controllers.
- Controllers should handle HTTP mapping, request validation boundaries, and response status; they should not contain substantial business logic.
- Use constructor injection and small, focused classes.
- Define explicit request and response DTOs for public APIs. Do not serialize JPA entities directly.
- Keep the REST API versioned under `/api/v1` when public API endpoints are introduced.
- Do not add a framework or abstraction without a concrete use case.
- When introducing a dependency, explain why it is needed and update the build configuration and documentation.
