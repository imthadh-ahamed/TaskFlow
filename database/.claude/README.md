# Claude Code database configuration

This directory contains project-level Claude Code guidance for the TaskFlow PostgreSQL component.

- `settings.json`: selected destructive-command deny rules.
- `rules/`: migration, SQL, procedure, security, testing, and scope conventions.
- `commands/`: reusable review and validation commands.
- `agents/`: optional SQL review and migration review agents.
- `skills/`: repeatable workflows for migrations and stored procedures.

These are guardrails, not a security sandbox. Review tool permission prompts and inspect database commands before approving them.
