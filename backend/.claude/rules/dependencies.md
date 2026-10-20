# Dependency and configuration rules

- Inspect `pom.xml` before assuming a dependency is available.
- Keep versions managed by the Spring Boot dependency management where possible.
- Prefer maintained, compatible dependencies with a clear purpose.
- Do not add dependencies solely for convenience when the JDK or existing stack is sufficient.
- Keep environment-specific configuration outside source control; commit safe examples, not real secrets.
- Use `application.properties` or `application.yml` consistently with the existing project.
- When adding a dependency, update tests and explain the reason in the final response.
