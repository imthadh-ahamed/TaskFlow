package com.taskflow.task.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class TaskStatusTest {

    // --- valid transitions ---

    @Test
    void todo_can_transition_to_in_progress() {
        assertThat(TaskStatus.TODO.canTransitionTo(TaskStatus.IN_PROGRESS)).isTrue();
    }

    @Test
    void in_progress_can_transition_to_in_review() {
        assertThat(TaskStatus.IN_PROGRESS.canTransitionTo(TaskStatus.IN_REVIEW)).isTrue();
    }

    @Test
    void in_progress_can_revert_to_todo() {
        assertThat(TaskStatus.IN_PROGRESS.canTransitionTo(TaskStatus.TODO)).isTrue();
    }

    @Test
    void in_review_can_transition_to_done() {
        assertThat(TaskStatus.IN_REVIEW.canTransitionTo(TaskStatus.DONE)).isTrue();
    }

    @Test
    void in_review_can_revert_to_in_progress() {
        assertThat(TaskStatus.IN_REVIEW.canTransitionTo(TaskStatus.IN_PROGRESS)).isTrue();
    }

    // --- invalid transitions ---

    @Test
    void todo_cannot_skip_to_in_review() {
        assertThat(TaskStatus.TODO.canTransitionTo(TaskStatus.IN_REVIEW)).isFalse();
    }

    @Test
    void todo_cannot_skip_to_done() {
        assertThat(TaskStatus.TODO.canTransitionTo(TaskStatus.DONE)).isFalse();
    }

    @Test
    void in_progress_cannot_skip_to_done() {
        assertThat(TaskStatus.IN_PROGRESS.canTransitionTo(TaskStatus.DONE)).isFalse();
    }

    @Test
    void in_review_cannot_transition_to_todo() {
        assertThat(TaskStatus.IN_REVIEW.canTransitionTo(TaskStatus.TODO)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    void done_is_terminal(TaskStatus next) {
        assertThat(TaskStatus.DONE.canTransitionTo(next)).isFalse();
    }
}
