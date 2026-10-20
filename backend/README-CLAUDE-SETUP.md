# Claude Code setup for TaskFlow backend

## Included files

- `CLAUDE.md`: project context and working agreement.
- `.mcp.json`: project-scoped MCP server configuration, intentionally empty to avoid enabling unreviewed external tools.
- `.claude/settings.json`: conservative deny rules for selected destructive commands.
- `.claude/rules/`: focused project rules.
- `.claude/commands/`: reusable `/review`, `/test`, and `/explain` commands.
- `.claude/agents/`: optional backend reviewer and test engineer.
- `.claude/skills/`: reusable feature and testing workflows.

## Install

1. Extract this archive.
2. Copy/merge the included `backend/` directory into your TaskFlow repository, or use these files to replace the corresponding configuration files in your existing backend.
3. Open Claude Code from the `backend/` directory for backend-focused work.
4. Review `CLAUDE.md` and update the project context as the dependencies and architecture evolve.
5. Keep `.mcp.json` empty until you have a concrete MCP server to configure. Add only servers you trust and understand.

## MCP filename note

Claude Code's project-level MCP configuration convention is `.mcp.json` (with a leading dot). This archive uses that filename rather than `mcp.json`.

## Important

The current `pom.xml` has Java 21 and Spring Boot 4.1.1 configured, with Spring MVC and test dependencies. PostgreSQL, Spring Data JPA, Flyway, Spring Security, Redis, Kafka, and OpenAPI are not currently declared. The guidance therefore tells Claude to inspect dependencies and not assume those components exist.

The settings are guardrails, not a replacement for reviewing tool permissions. Do not commit secrets or approve destructive commands without understanding their impact.
