-- V2__add_comments.sql
-- Adds the comments table for task-level collaboration.
-- Design decisions:
--   - A comment belongs to a task (cascade delete when task is removed).
--   - author_id is NOT NULL and references users with ON DELETE RESTRICT so
--     comment history is preserved even if the account is deactivated.
--   - body must not be blank (trim check) and is limited in the application layer.
--   - created_at / updated_at follow the same pattern as other tables.

create table public.comments (
    id          bigserial    primary key,
    task_id     bigint       not null,
    author_id   bigint       not null,
    body        text         not null,
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now(),

    constraint comments_task_fk
        foreign key (task_id) references public.tasks (id)
        on delete cascade,

    constraint comments_author_fk
        foreign key (author_id) references public.users (id)
        on delete restrict,

    constraint comments_body_not_blank
        check (trim(body) <> '')
);

-- Primary query pattern: "all comments on a task", ordered by creation time.
create index idx_comments_task_id
    on public.comments (task_id, created_at);
