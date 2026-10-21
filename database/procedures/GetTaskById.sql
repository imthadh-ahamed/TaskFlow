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
