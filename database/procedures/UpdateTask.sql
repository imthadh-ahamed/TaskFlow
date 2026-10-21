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
