-- V1__initial_schema.sql
-- Creates the initial schema: users, projects, project_members, tasks.
-- Design decisions documented in database/docs/schema-design.md.

-- ---------------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------------
create table public.users (
    id           bigserial    primary key,
    email        text         not null unique,
    display_name text         not null,
    is_active    boolean      not null default true,
    created_at   timestamptz  not null default now(),
    updated_at   timestamptz  not null default now(),

    -- email is stored normalized to lowercase; application must lowercase before insert/query.
    constraint users_email_lowercase check (email = lower(email))
);

-- ---------------------------------------------------------------------------
-- projects
-- ---------------------------------------------------------------------------
create table public.projects (
    id          bigserial    primary key,
    owner_id    bigint       not null,
    name        text         not null,
    description text,
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now(),

    constraint projects_owner_fk
        foreign key (owner_id) references public.users (id)
        on delete restrict
);

-- ---------------------------------------------------------------------------
-- project_members
-- Owners are also inserted here with role = 'owner' on project creation so
-- access-control queries need only check this table.
-- ---------------------------------------------------------------------------
create table public.project_members (
    project_id  bigint       not null,
    user_id     bigint       not null,
    role        text         not null,
    created_at  timestamptz  not null default now(),

    constraint project_members_pk
        primary key (project_id, user_id),

    constraint project_members_project_fk
        foreign key (project_id) references public.projects (id)
        on delete cascade,

    constraint project_members_user_fk
        foreign key (user_id) references public.users (id)
        on delete cascade,

    constraint project_members_role_values
        check (role in ('owner', 'member', 'viewer'))
);

-- ---------------------------------------------------------------------------
-- tasks
-- ---------------------------------------------------------------------------
create table public.tasks (
    id           bigserial    primary key,
    project_id   bigint       not null,
    assignee_id  bigint,
    title        text         not null,
    description  text,
    status       text         not null default 'todo',
    priority     text         not null default 'medium',
    due_date     date,
    created_at   timestamptz  not null default now(),
    updated_at   timestamptz  not null default now(),

    constraint tasks_project_fk
        foreign key (project_id) references public.projects (id)
        on delete cascade,

    -- assignee_id is nullable; SET NULL preserves the task when a user is deleted.
    constraint tasks_assignee_fk
        foreign key (assignee_id) references public.users (id)
        on delete set null,

    constraint tasks_status_values
        check (status in ('todo', 'in_progress', 'in_review', 'done')),

    constraint tasks_priority_values
        check (priority in ('low', 'medium', 'high', 'urgent'))
);

-- ---------------------------------------------------------------------------
-- Indexes
-- ---------------------------------------------------------------------------

-- project_members: reverse lookup — "which projects does this user belong to?"
create index idx_project_members_user_id
    on public.project_members (user_id);

-- tasks: primary query pattern — "all tasks in a project"
create index idx_tasks_project_id
    on public.tasks (project_id);

-- tasks: secondary query pattern — "all tasks assigned to a user"
create index idx_tasks_assignee_id
    on public.tasks (assignee_id);
