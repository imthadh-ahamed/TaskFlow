# Next.js and React rules

- Use the App Router conventions present in the installed Next.js version.
- Server Components are the default; add `"use client"` only when required.
- Keep client components small and close to the interactive UI that needs them.
- Keep server-only modules, secrets, and privileged operations out of client bundles.
- Prefer route-level composition and feature-oriented organization over oversized components.
- Avoid unnecessary `useEffect`, duplicated state, and premature abstractions.
- Use stable keys for mapped elements and avoid array indices when item identity can change.
- Follow Next.js guidance for metadata, route loading, error handling, and not-found pages when implementing features.
- Do not invent or assume backend routes; verify them from the backend or API documentation.
