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
