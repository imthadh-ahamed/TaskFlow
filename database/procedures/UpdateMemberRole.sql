CREATE OR REPLACE PROCEDURE public.UpdateMemberRole(
    IN    p_project_id BIGINT,
    IN    p_user_id    BIGINT,
    IN    p_role       TEXT,
    INOUT p_result     REFCURSOR DEFAULT 'update_member_cursor'
)
LANGUAGE plpgsql
AS $$
BEGIN
    OPEN p_result FOR
        WITH upd AS (
            UPDATE public.project_members
            SET    role = p_role
            WHERE  project_id = p_project_id
              AND  user_id    = p_user_id
            RETURNING project_id, user_id, role, created_at
        )
        SELECT * FROM upd;
END;
$$;
