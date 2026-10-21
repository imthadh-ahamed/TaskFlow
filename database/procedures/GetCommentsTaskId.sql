CREATE OR REPLACE PROCEDURE GetCommentsTaskId(
    IN p_task_id BIGINT,
    IN p_limit INT,
    IN p_offset INT,
    INOUT p_result REFCURSOR DEFAULT 'comments_cursor'
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
        LIMIT p_limit
        OFFSET p_offset;
END;
$$;