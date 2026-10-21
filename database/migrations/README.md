# migrations/

Versioned SQL migration files executed by Flyway in ascending version order.

## Naming Convention

```
V<version>__<description>.sql
```

- `<version>` — integer, unique, determines execution order (e.g. `1`, `2`, `10`)
- `<description>` — lowercase words separated by underscores

## Rules

- Files are **immutable** once applied to a shared environment. To correct a mistake, add a new migration.
- Each migration should be a single, focused change (one table, one index set, one procedure, etc.).
- Include comments inside the file explaining the intent of non-obvious changes.
- Test migrations locally against a clean database before committing.

## Applied migrations

| File | Description | Phase |
|------|-------------|-------|
| `V1__initial_schema.sql` | Creates `users`, `projects`, `project_members`, `tasks` tables with all constraints and indexes | T4.2 |
| `V2__add_comments.sql` | Creates `comments` table for task-level collaboration | T4.2 |
| `V3__add_auth_fields_to_users.sql` | Adds `password_hash`, `display_name` and auth fields to `users` | T9.x |
| `V4__add_refresh_tokens.sql` | Creates `refresh_tokens` table for JWT refresh-token rotation and reuse detection | T9.x |
| `V5__add_query_indexes.sql` | Adds composite indexes for filtered query patterns in JDBC repositories | T4.5 |
| `V6__stored_procedures.sql` | `create_project_with_owner` and `remove_project_member` — multi-step business workflows | T4.6 |
| `V7__get_comments_procedure.sql` | `GetCommentsTaskId` — paginated comment list via REFCURSOR | T11.x |
| `V8__project_procedures.sql` | Project CRUD procedures: `GetProjectById`, `GetAllProjects`, `GetProjectsByMemberId`, `CountProjectsByMemberId`, `CountAllProjects`, `UpdateProject`, `DeleteProject` | T11.x |
| `V9__member_procedures.sql` | Member procedures: `AddProjectMember`, `GetMembersByProjectId`, `GetMemberByProjectAndUser`, `UpdateMemberRole` | T11.x |
| `V10__task_procedures.sql` | Task CRUD procedures: `SaveTask`, `GetTaskById`, `GetTasksByProjectId`, `CountTasksByProjectId`, `UpdateTask`, `UpdateTaskStatus`, `GetTasksByProjectIdWithAssignee`, `GetTaskByIdWithAssignee` | T11.x |
| `V11__comment_procedures.sql` | `SaveComment`, `CountCommentsByTaskId` | T11.x |

## Pending migrations

None at this time. New migrations will be added as features are implemented.
