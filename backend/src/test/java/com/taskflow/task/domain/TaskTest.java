package com.taskflow.task.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class TaskTest {

    /** Minimal constructor helper — only the fields the domain rules care about. */
    private static Task task(String title, TaskStatus status, TaskPriority priority) {
        return new Task(null, null, null, title, null, status, priority, null, null, null);
    }

    // --- create() factory ---

    @Test
    void create_sets_todo_status_and_medium_priority_by_default() {
        Task task = Task.create("Fix login bug");

        assertThat(task.status()).isEqualTo(TaskStatus.TODO);
        assertThat(task.priority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(task.description()).isNull();
    }

    @Test
    void create_strips_surrounding_whitespace_from_title() {
        Task task = Task.create("  Fix login bug  ");
        assertThat(task.title()).isEqualTo("Fix login bug");
    }

    // --- title validation ---

    @Test
    void blank_title_is_rejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Task.create("   "))
                .withMessageContaining("blank");
    }

    @Test
    void null_title_is_rejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> task(null, TaskStatus.TODO, TaskPriority.MEDIUM))
                .withMessageContaining("blank");
    }

    @Test
    void title_at_max_length_is_accepted() {
        String title = "a".repeat(Task.MAX_TITLE_LENGTH);
        assertThat(task(title, TaskStatus.TODO, TaskPriority.MEDIUM).title())
                .hasSize(Task.MAX_TITLE_LENGTH);
    }

    @Test
    void title_exceeding_max_length_is_rejected() {
        String title = "a".repeat(Task.MAX_TITLE_LENGTH + 1);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> task(title, TaskStatus.TODO, TaskPriority.MEDIUM))
                .withMessageContaining("255");
    }

    // --- null status / priority ---

    @Test
    void null_status_is_rejected() {
        assertThatNullPointerException()
                .isThrownBy(() -> task("Title", null, TaskPriority.MEDIUM))
                .withMessageContaining("status");
    }

    @Test
    void null_priority_is_rejected() {
        assertThatNullPointerException()
                .isThrownBy(() -> task("Title", TaskStatus.TODO, null))
                .withMessageContaining("priority");
    }

    // --- transitionTo ---

    @Test
    void valid_transition_returns_new_task_with_updated_status() {
        Task task = Task.create("Deploy release");
        Task inProgress = task.transitionTo(TaskStatus.IN_PROGRESS);

        assertThat(inProgress.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(inProgress.title()).isEqualTo(task.title());
        assertThat(inProgress.priority()).isEqualTo(task.priority());
    }

    @Test
    void valid_transition_does_not_mutate_original_task() {
        Task original = Task.create("Deploy release");
        original.transitionTo(TaskStatus.IN_PROGRESS);

        assertThat(original.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void invalid_transition_throws() {
        Task task = Task.create("Deploy release");

        assertThatIllegalStateException()
                .isThrownBy(() -> task.transitionTo(TaskStatus.DONE))
                .withMessageContaining("TODO")
                .withMessageContaining("DONE");
    }

    @Test
    void transition_to_null_is_rejected() {
        Task task = Task.create("Deploy release");

        assertThatNullPointerException()
                .isThrownBy(() -> task.transitionTo(null));
    }

    @Test
    void full_happy_path_todo_to_done() {
        Task task = Task.create("Ship feature")
                .transitionTo(TaskStatus.IN_PROGRESS)
                .transitionTo(TaskStatus.IN_REVIEW)
                .transitionTo(TaskStatus.DONE);

        assertThat(task.status()).isEqualTo(TaskStatus.DONE);
    }
}
