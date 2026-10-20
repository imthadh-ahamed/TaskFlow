# Frontend API integration rules

- The Spring Boot backend is the source of business data; the frontend must not connect directly to PostgreSQL.
- Read the backend implementation or API contract before calling an endpoint.
- Use the configured API base URL from environment variables when API integration is introduced.
- Variables prefixed with `NEXT_PUBLIC_` are exposed to browser bundles; never put secrets in them.
- Type request and response payloads and handle loading, empty, success, and error states.
- Do not log access tokens, passwords, or sensitive user data.
- Do not add Axios, TanStack Query, authentication, or other API libraries unless the requested feature justifies them.
- Keep API integration out of unrelated UI components when a focused API module is appropriate.
