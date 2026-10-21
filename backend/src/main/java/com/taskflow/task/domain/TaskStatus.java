package com.taskflow.task.domain;

public enum TaskStatus {

    TODO {
        @Override
        public boolean canTransitionTo(TaskStatus next) {
            return next == IN_PROGRESS;
        }
    },
    IN_PROGRESS {
        @Override
        public boolean canTransitionTo(TaskStatus next) {
            return next == IN_REVIEW || next == TODO;
        }
    },
    IN_REVIEW {
        @Override
        public boolean canTransitionTo(TaskStatus next) {
            return next == DONE || next == IN_PROGRESS;
        }
    },
    DONE {
        @Override
        public boolean canTransitionTo(TaskStatus next) {
            return false;
        }
    };

    public abstract boolean canTransitionTo(TaskStatus next);
}
