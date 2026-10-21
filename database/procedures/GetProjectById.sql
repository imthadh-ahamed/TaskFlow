CREATE OR REPLACE PROCEDURE public.GetProjectById(
    IN    p_id     BIGINT,
    INOUT p_result REFCURSOR DEFAULT 'get_project_by_id_cursor'
)
LANGUAGE plpgsql
AS $$
BEGIN
    OPEN p_result FOR
        SELECT id, owner_id, name, description, created_at, updated_at
        FROM   public.projects
        WHERE  id = p_id;
END;
$$;
