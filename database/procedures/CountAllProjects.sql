CREATE OR REPLACE PROCEDURE public.CountAllProjects(
    INOUT p_count BIGINT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
BEGIN
    SELECT COUNT(*) INTO p_count FROM public.projects;
END;
$$;
