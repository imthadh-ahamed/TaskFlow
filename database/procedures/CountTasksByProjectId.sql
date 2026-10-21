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
