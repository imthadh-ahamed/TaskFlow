package com.taskflow.comment.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CommentTest {

    private static final Instant NOW = Instant.EPOCH;

    @Test
    void valid_comment_is_constructed_successfully() {
        Comment c = new Comment(1L, 1L, 1L, "Looks good", NOW, NOW);
        assertThat(c.body()).isEqualTo("Looks good");
        assertThat(c.taskId()).isEqualTo(1L);
        assertThat(c.authorId()).isEqualTo(1L);
    }

    @Test
    void body_is_stripped_of_surrounding_whitespace() {
        Comment c = new Comment(1L, 1L, 1L, "  hello  ", NOW, NOW);
        assertThat(c.body()).isEqualTo("hello");
    }

    @Test
    void blank_body_is_rejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Comment(1L, 1L, 1L, "   ", NOW, NOW))
                .withMessageContaining("blank");
    }

    @Test
    void null_body_is_rejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Comment(1L, 1L, 1L, null, NOW, NOW))
                .withMessageContaining("blank");
    }

    @Test
    void body_at_max_length_is_accepted() {
        String body = "a".repeat(Comment.MAX_BODY_LENGTH);
        assertThat(new Comment(1L, 1L, 1L, body, NOW, NOW).body())
                .hasSize(Comment.MAX_BODY_LENGTH);
    }

    @Test
    void body_exceeding_max_length_is_rejected() {
        String body = "a".repeat(Comment.MAX_BODY_LENGTH + 1);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Comment(1L, 1L, 1L, body, NOW, NOW))
                .withMessageContaining("5000");
    }

    @Test
    void null_created_at_is_rejected() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Comment(1L, 1L, 1L, "body", null, NOW))
                .withMessageContaining("createdAt");
    }

    @Test
    void null_updated_at_is_rejected() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Comment(1L, 1L, 1L, "body", NOW, null))
                .withMessageContaining("updatedAt");
    }
}
