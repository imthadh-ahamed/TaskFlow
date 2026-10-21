CREATE OR REPLACE PROCEDURE public.UpdateProject(
    IN    p_id          BIGINT,
    IN    p_name        TEXT,
    IN    p_description TEXT,
    INOUT p_result      REFCURSOR DEFAULT 'update_project_cursor'
)
LANGUAGE plpgsql
AS $$
BEGIN
    OPEN p_result FOR
        WITH upd AS (
            UPDATE public.projects
            SET    name        = p_name,
                   description = p_description,
                   updated_at  = now()
            WHERE  id = p_id
            RETURNING id, owner_id, name, description, created_at, updated_at
        )
        SELECT * FROM upd;
END;
$$;
