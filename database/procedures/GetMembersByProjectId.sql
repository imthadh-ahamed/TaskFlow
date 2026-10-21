CREATE OR REPLACE PROCEDURE public.GetMembersByProjectId(
    IN    p_project_id BIGINT,
    INOUT p_result     REFCURSOR DEFAULT 'get_members_cursor'
)
LANGUAGE plpgsql
AS $$
BEGIN
    OPEN p_result FOR
        SELECT project_id, user_id, role, created_at
        FROM   public.project_members
        WHERE  project_id = p_project_id
        ORDER BY created_at ASC;
END;
$$;
