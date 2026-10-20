# TaskFlow Frontend — Claude Code Guide

## Project context
This is the frontend component of the TaskFlow monorepo:
- `frontend/`: Next.js + React + TypeScript (this project)
- `backend/`: Java + Spring Boot REST API
- `database/`: PostgreSQL schema, migrations, and SQL assets

Work only within the frontend directory unless the user explicitly requests a monorepo-wide change. Do not modify backend, database, or root-level files for frontend-only tasks.

## Current scope
This project is initialized as a frontend boilerplate. Do not assume TaskFlow business features, authentication, API integration, or extra libraries exist yet. Inspect `package.json` and the source tree before using a dependency or describing existing behavior.

## Technology baseline
- Next.js App Router
- React
- TypeScript strict mode
- Tailwind CSS
- ESLint
- npm
- `@/*` import alias

The actual versions and configuration in the repository are authoritative.

## Working principles
1. Inspect existing files and scripts before editing.
2. Make the smallest complete change that satisfies the request.
3. Preserve the current Next.js conventions and configuration.
4. Prefer simple, readable React components and clear TypeScript types.
5. Keep components focused; separate reusable UI from feature-specific code as features are introduced.
6. Do not create abstractions, folders, hooks, or components without a concrete use case.
7. Do not add libraries unless needed for the requested task; explain dependency changes.
8. Do not implement unrequested product features during setup or refactoring.
9. Treat the Spring Boot API as the source of business data. The browser must never connect directly to PostgreSQL.
10. Keep environment-specific values in environment variables. Never commit credentials or secrets.

## React and Next.js standards
- Use App Router conventions and respect Server Components by default.
- Add `"use client"` only when browser APIs, state, effects, or event handlers require a Client Component.
- Do not turn an entire route tree into Client Components without a clear reason.
- Keep server-only code and secrets out of client bundles.
- Use appropriate loading, empty, error, and not-found states when implementing actual features.
- Use semantic HTML and accessible labels, keyboard behavior, focus states, and sufficient contrast.
- Use stable keys when rendering lists.
- Avoid unnecessary effects and duplicated state; derive values when possible.
- Use `next/image` for local or remote images when appropriate and configure remote patterns deliberately.
- Follow the project's installed ESLint and TypeScript rules rather than inventing conflicting conventions.

## Styling standards
- Use the existing Tailwind CSS setup and design tokens/conventions.
- Prefer responsive, mobile-first layouts.
- Avoid introducing a second styling system without approval.
- Keep global CSS minimal; use component-level utilities for local styling.
- Do not add visual libraries or icon packages unless requested or justified.

## API and environment configuration
- Use the configured backend API base URL from environment variables when API integration is requested.
- A `NEXT_PUBLIC_` variable is public and must never contain secrets.
- Do not assume a backend endpoint exists without inspecting backend code or API documentation.
- Keep API calls in a focused module when integration is introduced; handle loading, errors, and response types explicitly.
- Do not add API clients, TanStack Query, Axios, authentication, or state-management libraries unless the task requires them.

## Testing and validation
- Inspect available `package.json` scripts first.
- Run the relevant commands, commonly `npm run lint` and `npm run build`.
- Add tests only when requested or when the project already has an established test setup suitable for the change.
- Do not claim checks passed unless they were actually executed.
- If a command fails or cannot run, report the actual error and what remains unverified.

## Safe tool use
- Do not delete broad directories, overwrite user work, reset Git history, or run destructive commands without explicit permission.
- Do not modify backend/database files or shared root configuration during a frontend-only task.
- Ask before adding dependencies, changing package managers, changing Node requirements, or modifying deployment/cloud resources.
- Review changes to lockfiles and configuration carefully.

## Completion response
Summarize:
1. What changed
2. Dependencies added or removed, if any
3. Commands actually run and their results
4. Remaining limitations or follow-up steps
