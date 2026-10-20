---
name: add-frontend-feature
description: Implement a focused TaskFlow frontend feature using existing Next.js and TypeScript conventions.
---

# Add a frontend feature

1. Inspect the current app structure, `package.json`, existing UI, and relevant backend API contract.
2. Confirm the requested behavior and acceptance criteria.
3. Follow the existing App Router and feature organization.
4. Keep Server Components by default; add Client Components only where interactivity requires them.
5. Use strict TypeScript types and accessible semantic HTML.
6. Use existing Tailwind conventions; do not add a UI or icon library unless justified and approved.
7. If API integration is needed, verify the endpoint and use the configured base URL. Never connect directly to the database.
8. Add loading, error, empty, and success states where applicable.
9. Run available lint and build commands and report actual results.
10. Do not implement unrelated features.
