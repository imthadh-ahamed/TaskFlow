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
                (p_project_id, p_title, p_description, 'todo', p_priority, p_assignee_id, p_due_date)
            RETURNING id, project_id, assignee_id, title, description,
                      status, priority, due_date, created_at, updated_at
        )
        SELECT * FROM ins;
END;
$$;
