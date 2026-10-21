-- V9__member_procedures.sql
-- Stored procedures for project membership data access.
--
-- All procedures that return rows use REFCURSOR; callers must be @Transactional.
-- AddProjectMember and UpdateMemberRole use CTE-wrapped DML to return the
-- affected row through the cursor.
--
-- Calling from Java: JdbcMemberRepository.

-- ---------------------------------------------------------------------------
-- AddProjectMember — insert a member row and return it
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- GetMembersByProjectId — all members of a project, ordered by join date
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- GetMemberByProjectAndUser — single membership row (0 or 1)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE public.GetMemberByProjectAndUser(
    IN    p_project_id BIGINT,
    IN    p_user_id    BIGINT,
    INOUT p_result     REFCURSOR DEFAULT 'get_member_cursor'
)
LANGUAGE plpgsql
AS $$
BEGIN
    OPEN p_result FOR
        SELECT project_id, user_id, role, created_at
        FROM   public.project_members
        WHERE  project_id = p_project_id
          AND  user_id    = p_user_id;
END;
$$;

-- ---------------------------------------------------------------------------
-- UpdateMemberRole — update role and return the updated membership row
-- ---------------------------------------------------------------------------
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
