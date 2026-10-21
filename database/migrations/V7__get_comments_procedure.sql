-- V7__get_comments_procedure.sql
-- Stored procedure for paginated comment retrieval by task.
--
-- Design notes
-- ------------
-- Returns results through a PostgreSQL REFCURSOR so the caller can retrieve
-- the full result set in a single round-trip after the CALL. The cursor is
-- opened inside the caller's transaction and must be fetched before that
-- transaction commits. Spring's TransactionTemplate in JdbcCommentRepository
-- ensures the CALL and the subsequent FETCH happen in the same connection
-- transaction.
--
-- Calling from Java
-- -----------------
--   CALL public.GetCommentsTaskId(?, ?, ?, ?)
-- See JdbcCommentRepository.findByTaskId() for the JDBC implementation.

-- ---------------------------------------------------------------------------
-- GetCommentsTaskId
--
-- Purpose  : Return a paginated list of comments for a task, ordered by
--            creation time ascending (oldest first — natural comment thread
--            order). Pagination is enforced in the database so the cursor
--            carries only the requested slice.
--
-- Parameters
--   p_task_id (IN)    bigint     — task to fetch comments for
--   p_limit   (IN)    int        — maximum number of rows to return; must be > 0
--   p_offset  (IN)    int        — number of rows to skip; must be >= 0
--   p_result  (INOUT) refcursor  — opened cursor the caller fetches from;
--                                  defaults to 'comments_cursor'
--
-- Failure behavior
--   Raises an exception (SQLSTATE P0001) when p_limit <= 0 or p_offset < 0.
--   The application layer also validates these before calling so the check
--   here provides database-level defence-in-depth.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE public.GetCommentsTaskId(
    IN    p_task_id BIGINT,
    IN    p_limit   INT,
    IN    p_offset  INT,
    INOUT p_result  REFCURSOR DEFAULT 'comments_cursor'
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_limit <= 0 OR p_offset < 0 THEN
        RAISE EXCEPTION 'Invalid pagination parameters';
    END IF;

    OPEN p_result FOR
        SELECT
            id,
            task_id,
            author_id,
            body,
            created_at,
            updated_at
        FROM comments
        WHERE task_id = p_task_id
        ORDER BY created_at ASC, id ASC
        LIMIT  p_limit
        OFFSET p_offset;
END;
$$;
