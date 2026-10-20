# TypeScript rules

- Keep strict TypeScript settings enabled.
- Avoid `any`; prefer precise types, generics, `unknown` plus narrowing, and discriminated unions when useful.
- Type component props and API request/response data explicitly.
- Avoid unsafe type assertions unless justified and documented.
- Use `import type` for type-only imports when consistent with the project configuration.
- Handle nullable and optional values explicitly.
- Prefer small, understandable types over elaborate type-level machinery.
- Do not duplicate API types across many files; introduce shared types only when there is a real reuse boundary.
