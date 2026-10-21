CREATE OR REPLACE PROCEDURE public.DeleteProject(
    IN    p_id      BIGINT,
    INOUT p_deleted BIGINT DEFAULT 0
)
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM public.projects WHERE id = p_id;
    GET DIAGNOSTICS p_deleted = ROW_COUNT;
END;
$$;
