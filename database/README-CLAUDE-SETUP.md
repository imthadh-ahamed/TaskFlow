# TaskFlow Database — Claude Code Setup

## Included files

- `CLAUDE.md`: database-specific project instructions.
- `.mcp.json`: intentionally empty project-scoped MCP configuration.
- `.claude/settings.json`: selected destructive-command deny rules.
- `.claude/rules/`: PostgreSQL, migration, procedure/function, security, testing, naming, and scope conventions.
- `.claude/commands/`: `/review-sql`, `/validate-db`, and `/explain-sql`.
- `.claude/agents/`: SQL reviewer and migration reviewer.
- `.claude/skills/`: repeatable migration and stored procedure workflows.

## Install

1. Extract the ZIP.
2. Merge its contents into `TaskFlow/database/`.
3. Preserve existing SQL assets and review filename conflicts before replacing anything.
4. Open Claude Code from `database/` for database-focused work.
5. Keep shared monorepo guidance in the root `TaskFlow/CLAUDE.md`, if one exists.

## MCP configuration

Claude Code's project-level MCP configuration uses `.mcp.json`. It is intentionally empty; configure only trusted MCP servers you actually need.

## Scope

This package configures guidance only. It does not create tables, procedures, functions, views, indexes, migrations, database connections, or credentials. The deny rules are guardrails, not a security sandbox.
