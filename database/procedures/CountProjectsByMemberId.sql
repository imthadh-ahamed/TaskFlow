CREATE OR REPLACE PROCEDURE public.CountProjectsByMemberId(
    IN    p_user_id BIGINT,
    INOUT p_count   BIGINT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
BEGIN
    SELECT COUNT(*)
    INTO   p_count
    FROM   public.projects       p
    JOIN   public.project_members pm ON pm.project_id = p.id
    WHERE  pm.user_id = p_user_id;
END;
$$;
