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
