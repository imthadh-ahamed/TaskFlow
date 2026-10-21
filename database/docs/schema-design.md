# Initial Relational Model

**Phase:** T4.1 — Design  
**Status:** Approved — migration to follow in T4.2

This document defines the initial schema covering users, projects, memberships, and tasks. No comments, notifications, audit logs, or outbox tables are included.

---

## Entity Relationship Diagram

```
users
─────
id  PK
email  UNIQUE NOT NULL
display_name  NOT NULL
is_active  NOT NULL  DEFAULT true
created_at  NOT NULL  DEFAULT now()
updated_at  NOT NULL  DEFAULT now()

       │ (1)                  │ (1)
       │                      │ RESTRICT on delete
       │ (N)                  │ (N)
       ▼                      ▼
project_members          projects
───────────────          ────────
project_id  FK ◀────────── id  PK
user_id     FK ──▶ users   owner_id  FK → users  RESTRICT
role        NOT NULL        name  NOT NULL
created_at  NOT NULL        description  nullable
UNIQUE(project_id,          created_at  NOT NULL
       user_id)             updated_at  NOT NULL
                                │
                                │ CASCADE on delete
                                │ (N)
                                ▼
                            tasks
                            ─────
                            id  PK
                            project_id  FK  CASCADE
                            assignee_id  FK → users  SET NULL  (nullable)
                            title  NOT NULL
                            description  nullable
                            status  NOT NULL  DEFAULT 'todo'
                            priority  NOT NULL  DEFAULT 'medium'
                            due_date  date  (nullable)
                            created_at  NOT NULL
                            updated_at  NOT NULL
```

---

## Entity Definitions

### `users`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `bigserial` | `PRIMARY KEY` | |
| `email` | `text` | `NOT NULL UNIQUE CHECK (email = lower(email))` | Stored lowercase; app normalizes before insert/query |
| `display_name` | `text` | `NOT NULL` | Free-form display name |
| `is_active` | `boolean` | `NOT NULL DEFAULT true` | Soft account status; deactivated users cannot log in |
| `created_at` | `timestamptz` | `NOT NULL DEFAULT now()` | |
| `updated_at` | `timestamptz` | `NOT NULL DEFAULT now()` | Maintained by application on every update |

Password hash is deliberately omitted. It is added in the authentication phase with its own migration.

---

### `projects`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `bigserial` | `PRIMARY KEY` | |
| `owner_id` | `bigint` | `NOT NULL REFERENCES users(id) ON DELETE RESTRICT` | User who owns the project; see ownership decision below |
| `name` | `text` | `NOT NULL` | |
| `description` | `text` | nullable | |
| `created_at` | `timestamptz` | `NOT NULL DEFAULT now()` | |
| `updated_at` | `timestamptz` | `NOT NULL DEFAULT now()` | |

---

### `project_members`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `project_id` | `bigint` | `NOT NULL REFERENCES projects(id) ON DELETE CASCADE` | |
| `user_id` | `bigint` | `NOT NULL REFERENCES users(id) ON DELETE CASCADE` | |
| `role` | `text` | `NOT NULL CHECK (role IN ('owner', 'member', 'viewer'))` | |
| `created_at` | `timestamptz` | `NOT NULL DEFAULT now()` | |
| | | `PRIMARY KEY (project_id, user_id)` | Composite PK; one role per user per project |

---

### `tasks`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `bigserial` | `PRIMARY KEY` | |
| `project_id` | `bigint` | `NOT NULL REFERENCES projects(id) ON DELETE CASCADE` | |
| `assignee_id` | `bigint` | `REFERENCES users(id) ON DELETE SET NULL` | Nullable — unassigned tasks are valid |
| `title` | `text` | `NOT NULL` | |
| `description` | `text` | nullable | |
| `status` | `text` | `NOT NULL DEFAULT 'todo' CHECK (status IN ('todo', 'in_progress', 'in_review', 'done'))` | |
| `priority` | `text` | `NOT NULL DEFAULT 'medium' CHECK (priority IN ('low', 'medium', 'high', 'urgent'))` | |
| `due_date` | `date` | nullable | Date only, no time; UI displays in user's timezone |
| `created_at` | `timestamptz` | `NOT NULL DEFAULT now()` | |
| `updated_at` | `timestamptz` | `NOT NULL DEFAULT now()` | |

---

## Indexes

| Index name | Table | Columns | Reason |
|------------|-------|---------|--------|
| _(from UNIQUE)_ | `users` | `email` | Unique login lookup |
| `idx_project_members_user_id` | `project_members` | `user_id` | List projects a user belongs to |
| `idx_tasks_project_id` | `tasks` | `project_id` | List tasks in a project |
| `idx_tasks_assignee_id` | `tasks` | `assignee_id` | List tasks assigned to a user |

`project_members` already has a composite PK index on `(project_id, user_id)`, which covers "list members of a project" queries. A separate index on `user_id` alone is added for reverse lookups.

---

## Deletion Behavior

| Deleted row | Effect on related rows |
|-------------|----------------------|
| `users` | Blocked if user owns any project (`ON DELETE RESTRICT` on `projects.owner_id`). Transfer ownership first. |
| `users` | Cascades to `project_members` rows for that user. |
| `users` | Sets `tasks.assignee_id` to `NULL` for any tasks they were assigned to. |
| `projects` | Cascades to all `project_members` rows for that project. |
| `projects` | Cascades to all `tasks` rows in that project. |
| `tasks` | No cascades (tasks have no child tables yet). |

---

## Design Decisions

### Primary key type: `bigserial`
`bigserial` is chosen over `uuid` for simplicity in this phase. UUIDs are better for distributed ID generation but add index overhead and complicate debugging. This choice can be revisited in a future phase before the first public deployment.

### Status and priority: `text` with CHECK constraints, not PostgreSQL ENUMs
PostgreSQL ENUMs require `ALTER TYPE` to add values, and removing values is not supported without recreation. `text` with a `CHECK` constraint can be evolved with a plain `ALTER TABLE ... DROP CONSTRAINT / ADD CONSTRAINT`. The constraint is explicit, and the values are visible in the schema.

### Email normalization
Email is stored lowercase and a `CHECK (email = lower(email))` constraint enforces this at the database level. The application must normalize email to lowercase before any insert or query. This avoids duplicate accounts with differing case.

### Ownership and membership
`projects.owner_id` records who created the project. Owners are **also** inserted into `project_members` with `role = 'owner'` when the project is created. Benefits: access-control queries only need to check `project_members` — no second join against `projects.owner_id`; role changes are uniform across owner and non-owner members.

### `updated_at` maintenance
`updated_at` is set by the application layer on every update, not by a trigger. Triggers will be introduced only if the pattern proves unreliable across multiple callers. This keeps the schema simple in the initial phase.

### Timestamps: `timestamptz`
All timestamps use `timestamptz` (timestamp with time zone), stored in UTC. The application converts to the user's local timezone for display. `due_date` uses `date` because task due dates are conceptually calendar dates, not instants.

---

## Out of Scope

The following tables are explicitly deferred:

| Entity | Deferred to |
|--------|------------|
| `comments` | Comments phase |
| `notifications` | Notifications phase |
| `audit_log` | Audit/compliance phase |
| `outbox` | Event-driven messaging phase |
| `password_hash` column on `users` | Authentication phase (T6.x) |
