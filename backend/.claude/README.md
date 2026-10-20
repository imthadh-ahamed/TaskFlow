# Claude Code configuration

This directory contains project-level Claude Code guidance for the TaskFlow backend.

## Contents

- `settings.json`: conservative deny rules for a few destructive shell commands.
- `rules/`: focused coding, API, security, testing, database, and dependency standards.
- `commands/`: reusable slash commands such as `/review`, `/test`, and `/explain`.
- `agents/`: optional specialized reviewer and test-engineer agents.
- `skills/`: reusable workflows for adding a feature and writing tests.

## Use

Open Claude Code from the backend directory, or from the monorepo root if the root `CLAUDE.md` describes the monorepo and links to backend-specific guidance.

Review permission prompts before approving commands. These settings are guardrails, not a security sandbox. Adjust them to your team's needs and Claude Code version.
