-- V10__task_procedures.sql
-- Stored procedures for task data access.
--
-- Nullable filter parameters (p_status, p_priority, p_assignee_id) replace
-- the dynamic SQL that was previously built in Java. Passing NULL for any
-- filter means "no filter"; the WHERE clause uses (param IS NULL OR col = param)
-- so the query planner sees a stable shape.
--
-- GetTasksByProjectIdWithAssignee and GetTaskByIdWithAssignee include a
-- LEFT JOIN on users to return assignee_name in the same query, avoiding
-- the N+1 that would arise from a per-task user lookup.
--
-- Calling from Java: JdbcTaskRepository.

-- ---------------------------------------------------------------------------
-- SaveTask — insert a task and return the new row
-- ---------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE public.SaveTask(
    IN    p_project_id  BIGINT,
    IN    p_title       TEXT,
    IN    p_description TEXT,
    IN    p_priority    TEXT,
    IN    p_assignee_id BIGINT,
    IN    p_due_date    DATE,
    INOUT p_result      REFCURSOR DEFAULT 'save_task_cursor'
)
LANGUAGE plpgsql
AS $$
BEGIN
    OPEN p_result FOR
        WITH ins AS (
            INSERT INTO public.tasks
                (project_id, title, description, status, priority, assignee_id, due_date)
            VALUES
                (p_project_id, p_title, p_description, 'todo', p_priority,
                 p_assignee_id, p_due_date)
            RETURNING id, project_id, assignee_id, title, description,
                      status, priority, due_date, created_at, updated_at
        )
        SELECT * FROM ins;
END;
$$;

-- ---------------------------------------------------------------------------
-- GetTaskById — 0 or 1 task rows by primary key
-- ---------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE public.GetTaskById(
    IN    p_id     BIGINT,
    INOUT p_result REFCURSOR DEFAULT 'get_task_by_id_cursor'
)
LANGUAGE plpgsql
AS $$
BEGIN
    OPEN p_result FOR
        SELECT id, project_id, assignee_id, title, description,
               status, priority, due_date, created_at, updated_at
        FROM   public.tasks
        WHERE  id = p_id;
END;
$$;

-- ---------------------------------------------------------------------------
-- GetTasksByProjectId — paginated tasks with optional filters
-- ---------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE public.GetTasksByProjectId(
    IN    p_project_id   BIGINT,
    IN    p_status       TEXT,
    IN    p_priority     TEXT,
    IN    p_assignee_id  BIGINT,
    IN    p_limit        INT,
    IN    p_offset       INT,
    INOUT p_result       REFCURSOR DEFAULT 'get_tasks_cursor'
)
LANGUAGE plpgsql
AS $$
BEGIN
    OPEN p_result FOR
        SELECT id, project_id, assignee_id, title, description,
               status, priority, due_date, created_at, updated_at
        FROM   public.tasks
        WHERE  project_id  = p_project_id
          AND  (p_status      IS NULL OR status      = p_status)
          AND  (p_priority    IS NULL OR priority    = p_priority)
          AND  (p_assignee_id IS NULL OR assignee_id = p_assignee_id)
        ORDER BY created_at DESC, id DESC
        LIMIT  p_limit
        OFFSET p_offset;
END;
$$;

-- ---------------------------------------------------------------------------
-- CountTasksByProjectId — count with the same optional filters
-- ---------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE public.CountTasksByProjectId(
    IN    p_project_id  BIGINT,
    IN    p_status      TEXT,
    IN    p_priority    TEXT,
    IN    p_assignee_id BIGINT,
    INOUT p_count       BIGINT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
BEGIN
    SELECT COUNT(*)
    INTO   p_count
    FROM   public.tasks
    WHERE  project_id  = p_project_id
      AND  (p_status      IS NULL OR status      = p_status)
      AND  (p_priority    IS NULL OR priority    = p_priority)
      AND  (p_assignee_id IS NULL OR assignee_id = p_assignee_id);
END;
$$;

-- ---------------------------------------------------------------------------
-- UpdateTask — update mutable fields and return the updated row
-- ---------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE public.UpdateTask(
    IN    p_id          BIGINT,
    IN    p_title       TEXT,
    IN    p_description TEXT,
    IN    p_priority    TEXT,
    IN    p_assignee_id BIGINT,
    IN    p_due_date    DATE,
    INOUT p_result      REFCURSOR DEFAULT 'update_task_cursor'
)
LANGUAGE plpgsql
AS $$
BEGIN
    OPEN p_result FOR
        WITH upd AS (
            UPDATE public.tasks
            SET    title       = p_title,
                   description = p_description,
                   priority    = p_priority,
                   assignee_id = p_assignee_id,
                   due_date    = p_due_date,
                   updated_at  = now()
            WHERE  id = p_id
            RETURNING id, project_id, assignee_id, title, description,
                      status, priority, due_date, created_at, updated_at
        )
        SELECT * FROM upd;
END;
$$;

-- ---------------------------------------------------------------------------
-- UpdateTaskStatus — update status field and return the updated row
-- ---------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE public.UpdateTaskStatus(
    IN    p_id     BIGINT,
    IN    p_status TEXT,
    INOUT p_result REFCURSOR DEFAULT 'update_task_status_cursor'
)
LANGUAGE plpgsql
AS $$
BEGIN
    OPEN p_result FOR
        WITH upd AS (
            UPDATE public.tasks
            SET    status     = p_status,
                   updated_at = now()
            WHERE  id = p_id
            RETURNING id, project_id, assignee_id, title, description,
                      status, priority, due_date, created_at, updated_at
        )
        SELECT * FROM upd;
END;
$$;

-- ---------------------------------------------------------------------------
-- GetTasksByProjectIdWithAssignee — paginated tasks + assignee display name
-- Avoids N+1: includes LEFT JOIN users so assignee_name comes in one query.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE public.GetTasksByProjectIdWithAssignee(
    IN    p_project_id  BIGINT,
    IN    p_status      TEXT,
    IN    p_priority    TEXT,
    IN    p_assignee_id BIGINT,
    IN    p_limit       INT,
    IN    p_offset      INT,
    INOUT p_result      REFCURSOR DEFAULT 'get_tasks_with_assignee_cursor'
)
LANGUAGE plpgsql
AS $$
BEGIN
    OPEN p_result FOR
        SELECT t.id, t.project_id, t.assignee_id,
               t.title, t.description, t.status, t.priority,
               t.due_date, t.created_at, t.updated_at,
               u.display_name AS assignee_name
        FROM   public.tasks t
        LEFT JOIN public.users u ON u.id = t.assignee_id
        WHERE  t.project_id  = p_project_id
          AND  (p_status      IS NULL OR t.status      = p_status)
          AND  (p_priority    IS NULL OR t.priority    = p_priority)
          AND  (p_assignee_id IS NULL OR t.assignee_id = p_assignee_id)
        ORDER BY t.created_at DESC, t.id DESC
        LIMIT  p_limit
        OFFSET p_offset;
END;
$$;

-- ---------------------------------------------------------------------------
-- GetTaskByIdWithAssignee — single task + assignee display name
-- ---------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE public.GetTaskByIdWithAssignee(
    IN    p_id     BIGINT,
    INOUT p_result REFCURSOR DEFAULT 'get_task_with_assignee_cursor'
)
LANGUAGE plpgsql
AS $$
BEGIN
    OPEN p_result FOR
        SELECT t.id, t.project_id, t.assignee_id,
               t.title, t.description, t.status, t.priority,
               t.due_date, t.created_at, t.updated_at,
               u.display_name AS assignee_name
        FROM   public.tasks t
        LEFT JOIN public.users u ON u.id = t.assignee_id
        WHERE  t.id = p_id;
END;
$$;
