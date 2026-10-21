package com.taskflow.comment.application;

import com.taskflow.comment.api.CommentResponse;
import com.taskflow.comment.api.CreateCommentRequest;
import com.taskflow.comment.domain.Comment;
import com.taskflow.comment.infrastructure.JdbcCommentRepository;
import com.taskflow.common.ForbiddenException;
import com.taskflow.common.ResourceNotFoundException;
import com.taskflow.common.api.PageResponse;
import com.taskflow.membership.domain.MemberRole;
import com.taskflow.membership.domain.ProjectMember;
import com.taskflow.membership.infrastructure.JdbcMemberRepository;
import com.taskflow.task.domain.Task;
import com.taskflow.task.infrastructure.JdbcTaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    private final JdbcCommentRepository commentRepository;
    private final JdbcTaskRepository taskRepository;
    private final JdbcMemberRepository memberRepository;

    public CommentService(JdbcCommentRepository commentRepository,
                          JdbcTaskRepository taskRepository,
                          JdbcMemberRepository memberRepository) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.memberRepository = memberRepository;
    }

    public CommentResponse createComment(long taskId, CreateCommentRequest req, long callerId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task " + taskId + " not found"));
        requireWriteAccess(task.projectId(), callerId);
        Comment saved = commentRepository.save(taskId, callerId, req.body().strip());
        return toResponse(saved);
    }

    public PageResponse<CommentResponse> listComments(long taskId, int page, int size, long callerId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task " + taskId + " not found"));
        requireMember(task.projectId(), callerId);
        long total = commentRepository.countByTaskId(taskId);
        List<CommentResponse> content = commentRepository
                .findByTaskId(taskId, size, page * size)
                .stream().map(this::toResponse).toList();
        return PageResponse.of(content, page, size, total);
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

    private CommentResponse toResponse(Comment c) {
        return new CommentResponse(
                c.id(), c.taskId(), c.authorId(),
                c.body(),
                c.createdAt().toString(),
                c.updatedAt().toString()
        );
    }
}
