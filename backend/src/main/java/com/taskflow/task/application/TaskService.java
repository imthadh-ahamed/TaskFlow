package com.taskflow.task.application;

import com.taskflow.common.ForbiddenException;
import com.taskflow.common.ResourceNotFoundException;
import com.taskflow.common.api.PageResponse;
import com.taskflow.membership.domain.MemberRole;
import com.taskflow.membership.domain.ProjectMember;
import com.taskflow.membership.infrastructure.JdbcMemberRepository;
import com.taskflow.notification.domain.NotificationEvent;
import com.taskflow.notification.infrastructure.JdbcOutboxRepository;
import com.taskflow.project.infrastructure.JdbcProjectRepository;
import com.taskflow.task.api.*;
import com.taskflow.task.domain.Task;
import com.taskflow.task.domain.TaskPriority;
import com.taskflow.task.domain.TaskStatus;
import com.taskflow.task.infrastructure.JdbcTaskRepository;
import com.taskflow.task.infrastructure.TaskWithAssignee;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class TaskService {

    private final JdbcTaskRepository taskRepository;
    private final JdbcProjectRepository projectRepository;
    private final JdbcMemberRepository memberRepository;
    private final JdbcOutboxRepository outboxRepository;

    public TaskService(JdbcTaskRepository taskRepository,
                       JdbcProjectRepository projectRepository,
                       JdbcMemberRepository memberRepository,
                       JdbcOutboxRepository outboxRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.outboxRepository = outboxRepository;
    }

    public TaskResponse createTask(long projectId, CreateTaskRequest req, long callerId) {
        requireProjectExists(projectId);
        requireWriteAccess(projectId, callerId);
        TaskPriority priority = req.priority() != null ? req.priority() : TaskPriority.MEDIUM;
        LocalDate dueDate = parseDate(req.dueDate());
        Task saved = taskRepository.save(projectId, req.title().strip(),
                req.description(), priority, req.assigneeId(), dueDate);
        outboxRepository.insert(
                NotificationEvent.taskCreated(projectId, saved.id(), callerId, saved.assigneeId()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> listTasks(long projectId,
                                                String statusParam,
                                                String priorityParam,
                                                Long assigneeId,
                                                int page, int size,
                                                long callerId) {
        requireProjectExists(projectId);
        requireMember(projectId, callerId);
        TaskStatus statusFilter = parseStatus(statusParam);
        TaskPriority priorityFilter = parsePriority(priorityParam);
        long total = taskRepository.countByProjectId(
                projectId, statusFilter, priorityFilter, assigneeId);
        List<TaskResponse> content = taskRepository
                .findByProjectIdWithAssignee(projectId, statusFilter, priorityFilter, assigneeId,
                        size, page * size)
                .stream().map(ta -> toResponse(ta.task(), ta.assigneeName())).toList();
        return PageResponse.of(content, page, size, total);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(long taskId, long callerId) {
        TaskWithAssignee ta = taskRepository.findByIdWithAssignee(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task " + taskId + " not found"));
        requireMember(ta.task().projectId(), callerId);
        return toResponse(ta.task(), ta.assigneeName());
    }

    public TaskResponse updateTask(long taskId, UpdateTaskRequest req, long callerId) {
        Task existing = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task " + taskId + " not found"));
        requireWriteAccess(existing.projectId(), callerId);

        String newTitle = (req.title() != null) ? req.title().strip() : existing.title();
        if (newTitle.isBlank()) throw new IllegalArgumentException("Task title must not be blank");
        if (newTitle.length() > Task.MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Task title must not exceed 255 characters");
        }

        String newDescription = (req.description() == null)
                ? existing.description()
                : (req.description().isBlank() ? null : req.description().strip());

        TaskPriority newPriority = (req.priority() != null) ? req.priority() : existing.priority();

        Long newAssigneeId;
        boolean clearAssignee = Boolean.TRUE.equals(req.clearAssignee());
        if (clearAssignee) {
            newAssigneeId = null;
        } else {
            newAssigneeId = (req.assigneeId() != null) ? req.assigneeId() : existing.assigneeId();
        }

        LocalDate newDueDate;
        boolean clearDueDate = false;
        if (req.dueDate() == null) {
            newDueDate = existing.dueDate();
        } else if (req.dueDate().isBlank()) {
            newDueDate = null;
            clearDueDate = true;
        } else {
            newDueDate = parseDate(req.dueDate());
        }

        Task updated = taskRepository.update(taskId, newTitle, newDescription,
                newPriority, newAssigneeId, clearAssignee, newDueDate, clearDueDate)
                .orElseThrow(() -> new ResourceNotFoundException("Task " + taskId + " not found"));

        if (!Objects.equals(existing.assigneeId(), updated.assigneeId())) {
            outboxRepository.insert(NotificationEvent.taskAssigned(
                    existing.projectId(), taskId, callerId,
                    existing.assigneeId(), updated.assigneeId()));
        }
        return toResponse(updated);
    }

    public TaskResponse updateStatus(long taskId, UpdateTaskStatusRequest req, long callerId) {
        Task existing = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task " + taskId + " not found"));
        requireWriteAccess(existing.projectId(), callerId);
        existing.transitionTo(req.status());
        Task updated = taskRepository.updateStatus(taskId, req.status())
                .orElseThrow(() -> new ResourceNotFoundException("Task " + taskId + " not found"));
        return toResponse(updated);
    }

    // --- helpers ---

    private void requireProjectExists(long projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + projectId + " not found"));
    }

    private void requireMember(long projectId, long callerId) {
        memberRepository.findByProjectAndUser(projectId, callerId)
                .orElseThrow(ForbiddenException::new);
    }

    private void requireWriteAccess(long projectId, long callerId) {
        ProjectMember m = memberRepository.findByProjectAndUser(projectId, callerId)
                .orElseThrow(ForbiddenException::new);
        if (m.role() == MemberRole.VIEWER) {
            throw new ForbiddenException();
        }
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("dueDate must be in format yyyy-MM-dd");
        }
    }

    private static TaskStatus parseStatus(String value) {
        if (value == null) return null;
        try {
            return TaskStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid status filter '" + value + "'. Valid values: todo, in_progress, in_review, done");
        }
    }

    private static TaskPriority parsePriority(String value) {
        if (value == null) return null;
        try {
            return TaskPriority.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid priority filter '" + value + "'. Valid values: low, medium, high, urgent");
        }
    }

    private static TaskResponse toResponse(Task t) {
        return toResponse(t, null);
    }

    private static TaskResponse toResponse(Task t, String assigneeName) {
        return new TaskResponse(
                t.id(), t.projectId(), t.assigneeId(), assigneeName,
                t.title(), t.description(),
                t.status().name().toLowerCase(),
                t.priority().name().toLowerCase(),
                t.dueDate() != null ? t.dueDate().toString() : null,
                t.createdAt() != null ? t.createdAt().toString() : null,
                t.updatedAt() != null ? t.updatedAt().toString() : null
        );
    }
}
