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
