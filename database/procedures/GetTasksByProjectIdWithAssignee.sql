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
