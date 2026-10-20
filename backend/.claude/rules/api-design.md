# REST API design rules

- Use resource-oriented URLs and the correct HTTP methods/status codes.
- Keep request validation explicit and return useful, non-sensitive validation errors.
- Use DTOs for API request/response contracts; avoid exposing internal entity fields.
- Use consistent error response shapes and central exception handling as the API grows.
- Return `201 Created` for successful resource creation where appropriate, and use `Location` when practical.
- Bound list endpoints with pagination; validate page size and sorting fields.
- Do not trust user-supplied IDs as proof of access. Check ownership and authorization at the service boundary.
- Avoid leaking SQL, stack traces, file paths, secrets, or internal implementation details in API errors.
- Treat API contract changes as compatibility-sensitive and update tests/docs alongside them.
