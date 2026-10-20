# Frontend testing and verification rules

- Inspect `package.json` before choosing commands; do not assume scripts or test frameworks exist.
- Run `npm run lint` and `npm run build` for meaningful changes when available.
- Add tests using the existing test framework if one is already configured.
- Do not add a test framework solely for a boilerplate-only task unless requested.
- Cover user-visible behavior and important edge cases when implementing features.
- Avoid flaky tests, arbitrary timeouts, and order-dependent state.
- Never remove or weaken tests merely to get a green run.
- Report exactly which commands were run and their actual results.
