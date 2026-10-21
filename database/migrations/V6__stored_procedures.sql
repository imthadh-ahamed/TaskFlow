-- V6__stored_procedures.sql
-- PostgreSQL stored procedures for business-oriented database workflows.
--
-- Design philosophy
-- -----------------
-- Procedures are added only where they encapsulate a meaningful multi-step
-- database operation or enforce a business invariant that would otherwise rely
-- on the application layer alone. Single-statement CRUD remains in the
-- repository layer.
--
-- Reference patterns (from the CreateUser / GetAllUserStates SQL Server examples)
--   CreateUser:  validate → upsert primary record → insert related record → return result
--   Simpler ops: dedicated routine for a meaningful read or write with stable column contract
--
-- Transaction handling
-- --------------------
-- Neither procedure issues explicit COMMIT / ROLLBACK. Both rely on the
-- calling context's transaction. PostgreSQL rolls back all work inside a
-- PL/pgSQL block automatically when an unhandled exception propagates.
-- Spring's JdbcTemplate wraps each CALL in the active connection transaction,
-- so @Transactional on the service layer applies correctly.
--
-- Calling from Java
-- -----------------
-- Both procedures are called via JDBC CallableStatement:
--   CALL public.create_project_with_owner(?, ?, ?, ?, ?, ?)
--   CALL public.remove_project_member(?, ?)
-- See JdbcProjectRepository and JdbcMemberRepository for details.

-- ---------------------------------------------------------------------------
-- create_project_with_owner
--
-- Purpose  : Atomically create a project and register the creating user as
--            its OWNER in project_members. Before this procedure the two
--            INSERT statements happened as separate JDBC calls in the service
--            layer, leaving a window where the project could exist without an
--            owner if the second insert failed.
--
-- Parameters
--   p_owner_id    (IN)    bigint     — user who will own the project; must
--                                      exist in public.users (FK enforced)
--   p_name        (IN)    text       — project name (non-blank, max 200 chars
--                                      enforced by the application before call)
--   p_description (IN)    text       — optional description; may be NULL
--   p_id          (INOUT) bigint     — populated with the generated project id
--   p_created_at  (INOUT) timestamptz — populated with the row's created_at
--   p_updated_at  (INOUT) timestamptz — populated with the row's updated_at
--
-- Side effects
--   1. Inserts one row into public.projects.
--   2. Inserts one row into public.project_members with role = 'owner'.
--
-- Failure behavior
--   If p_owner_id does not reference a row in public.users, the FK on
--   projects.owner_id fails and the entire operation is rolled back.
--   If project_members already contains (p_id, p_owner_id) the PK
--   constraint fails and the entire operation is rolled back.
-- ---------------------------------------------------------------------------
CREATE PROCEDURE public.create_project_with_owner(
    IN    p_owner_id    bigint,
    IN    p_name        text,
    IN    p_description text,
    INOUT p_id          bigint       DEFAULT NULL,
    INOUT p_created_at  timestamptz  DEFAULT NULL,
    INOUT p_updated_at  timestamptz  DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO public.projects (owner_id, name, description)
    VALUES (p_owner_id, p_name, p_description)
    RETURNING id, created_at, updated_at
    INTO p_id, p_created_at, p_updated_at;

    INSERT INTO public.project_members (project_id, user_id, role)
    VALUES (p_id, p_owner_id, 'owner');
END;
$$;

-- ---------------------------------------------------------------------------
-- remove_project_member
--
-- Purpose  : Remove a member from a project, enforcing that the project owner
--            (role = 'owner') cannot be removed through this procedure. The
--            same rule is checked in the application service layer; this
--            procedure provides database-level defense-in-depth so the
--            invariant holds even if the service check is bypassed.
--
-- Parameters
--   p_project_id (IN) bigint — the project the member belongs to
--   p_user_id    (IN) bigint — the user to remove
--
-- Failure behavior
--   Raises an exception (SQLSTATE P0001) if:
--     • The (p_project_id, p_user_id) row does not exist in project_members.
--     • The row has role = 'owner'.
--   The caller's transaction is not modified; the exception propagates through
--   JDBC as a DataAccessException.
-- ---------------------------------------------------------------------------
CREATE PROCEDURE public.remove_project_member(
    IN p_project_id bigint,
    IN p_user_id    bigint
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_role text;
BEGIN
    SELECT role INTO v_role
    FROM public.project_members
    WHERE project_id = p_project_id AND user_id = p_user_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Member (project_id=%, user_id=%) not found in project_members',
            p_project_id, p_user_id;
    END IF;

    IF v_role = 'owner' THEN
        RAISE EXCEPTION 'Cannot remove the project owner (project_id=%, user_id=%)',
            p_project_id, p_user_id;
    END IF;

    DELETE FROM public.project_members
    WHERE project_id = p_project_id AND user_id = p_user_id;
END;
$$;
