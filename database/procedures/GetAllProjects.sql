CREATE OR REPLACE PROCEDURE public.GetAllProjects(
    IN    p_limit  INT,
    IN    p_offset INT,
    INOUT p_result REFCURSOR DEFAULT 'get_all_projects_cursor'
)
LANGUAGE plpgsql
AS $$
BEGIN
    OPEN p_result FOR
        SELECT id, owner_id, name, description, created_at, updated_at
        FROM   public.projects
        ORDER BY created_at DESC, id DESC
        LIMIT  p_limit
        OFFSET p_offset;
END;
$$;
