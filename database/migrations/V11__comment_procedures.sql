-- V11__comment_procedures.sql
-- Remaining comment procedures: SaveComment and CountCommentsByTaskId.
-- GetCommentsTaskId (paginated list) was introduced in V7.
--
-- Calling from Java: JdbcCommentRepository.

-- ---------------------------------------------------------------------------
-- SaveComment — insert a comment and return the new row
-- ---------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE public.SaveComment(
    IN    p_task_id   BIGINT,
    IN    p_author_id BIGINT,
    IN    p_body      TEXT,
    INOUT p_result    REFCURSOR DEFAULT 'save_comment_cursor'
)
LANGUAGE plpgsql
AS $$
BEGIN
    OPEN p_result FOR
        WITH ins AS (
            INSERT INTO public.comments (task_id, author_id, body)
            VALUES (p_task_id, p_author_id, p_body)
            RETURNING id, task_id, author_id, body, created_at, updated_at
        )
        SELECT * FROM ins;
END;
$$;

-- ---------------------------------------------------------------------------
-- CountCommentsByTaskId — total comments for a task
-- ---------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE public.CountCommentsByTaskId(
    IN    p_task_id BIGINT,
    INOUT p_count   BIGINT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
BEGIN
    SELECT COUNT(*) INTO p_count
    FROM   public.comments
    WHERE  task_id = p_task_id;
END;
$$;
