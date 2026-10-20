# TaskFlow Frontend — Claude Code Setup

## Included

- `CLAUDE.md`: frontend project instructions.
- `.mcp.json`: empty project-scoped MCP configuration placeholder.
- `.claude/settings.json`: selected destructive-command deny rules.
- `.claude/rules/`: Next.js/React, TypeScript, accessibility, styling, API integration, testing, and dependency standards.
- `.claude/commands/`: `/review`, `/validate`, and `/explain`.
- `.claude/agents/`: frontend reviewer and test engineer.
- `.claude/skills/`: feature implementation and boilerplate-only initialization workflows.

## Install

1. Extract the ZIP.
2. Merge the included files into your `TaskFlow/frontend/` directory.
3. Preserve your existing source files, `package.json`, and lockfile. Review before replacing any file with the same name.
4. Open Claude Code from `frontend/` for frontend-specific work.
5. Keep root-level monorepo instructions in `TaskFlow/CLAUDE.md` if you create one later.

## MCP configuration

Claude Code's project-level MCP configuration filename is `.mcp.json`. It is intentionally empty. Add only MCP servers you trust and actually need.

## Scope and safety

This configuration is designed for a frontend boilerplate. It does not add product features or dependencies. Permission deny rules are guardrails, not a complete security boundary. Review commands and permission prompts before approving them.
