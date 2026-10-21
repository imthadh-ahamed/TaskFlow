CREATE OR REPLACE PROCEDURE public.CountCommentsByTaskId(
    IN    p_task_id BIGINT,
    INOUT p_count   BIGINT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
BEGIN
    SELECT COUNT(*) INTO p_count
    FROM   public.comments
    WHERE  task_id = p_task_id;
END;
$$;
