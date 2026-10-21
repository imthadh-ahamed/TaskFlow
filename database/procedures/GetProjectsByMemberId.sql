CREATE OR REPLACE PROCEDURE public.GetProjectsByMemberId(
    IN    p_user_id BIGINT,
    IN    p_limit   INT,
    IN    p_offset  INT,
    INOUT p_result  REFCURSOR DEFAULT 'get_projects_by_member_cursor'
)
LANGUAGE plpgsql
AS $$
BEGIN
    OPEN p_result FOR
        SELECT p.id, p.owner_id, p.name, p.description, p.created_at, p.updated_at
        FROM   public.projects       p
        JOIN   public.project_members pm ON pm.project_id = p.id
        WHERE  pm.user_id = p_user_id
        ORDER BY p.created_at DESC, p.id DESC
        LIMIT  p_limit
        OFFSET p_offset;
END;
$$;
