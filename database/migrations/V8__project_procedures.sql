-- V8__project_procedures.sql
-- Stored procedures for project data access.
--
-- All list/read procedures return results through a PostgreSQL REFCURSOR so
-- callers retrieve a full result set in one round-trip after the CALL. The
-- CALL and the subsequent FETCH ALL must happen within the same connection
-- transaction; callers are expected to be @Transactional.
--
-- Write procedures (UpdateProject, DeleteProject) do not return a cursor.
-- UpdateProject uses a CTE to return the updated row via REFCURSOR.
-- DeleteProject returns the affected-row count as INOUT p_deleted BIGINT.
--
-- Calling from Java: JdbcProjectRepository — each repository method now
-- delegates to the corresponding procedure via JDBC CallableStatement.

-- ---------------------------------------------------------------------------
-- GetProjectById — return 0 or 1 project rows by primary key
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- GetAllProjects — paginated list of all projects
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- GetProjectsByMemberId — paginated projects the user belongs to
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- CountProjectsByMemberId — total projects the user belongs to
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- CountAllProjects — total project count
-- ---------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE public.CountAllProjects(
    INOUT p_count BIGINT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
BEGIN
    SELECT COUNT(*) INTO p_count FROM public.projects;
END;
$$;

-- ---------------------------------------------------------------------------
-- UpdateProject — update name/description and return the updated row
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- DeleteProject — delete by id; p_deleted = 1 if row was found and removed
-- ---------------------------------------------------------------------------
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
