CREATE OR REPLACE PROCEDURE public.AddProjectMember(
    IN    p_project_id BIGINT,
    IN    p_user_id    BIGINT,
    IN    p_role       TEXT,
    INOUT p_result     REFCURSOR DEFAULT 'add_member_cursor'
)
LANGUAGE plpgsql
AS $$
BEGIN
    OPEN p_result FOR
        WITH ins AS (
            INSERT INTO public.project_members (project_id, user_id, role)
            VALUES (p_project_id, p_user_id, p_role)
            RETURNING project_id, user_id, role, created_at
        )
        SELECT * FROM ins;
END;
$$;
