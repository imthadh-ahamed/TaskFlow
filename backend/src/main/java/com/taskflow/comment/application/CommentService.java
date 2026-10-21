package com.taskflow.comment.application;

import com.taskflow.comment.api.CommentResponse;
import com.taskflow.comment.api.CreateCommentRequest;
import com.taskflow.comment.domain.Comment;
import com.taskflow.comment.infrastructure.JdbcCommentRepository;
import com.taskflow.common.ResourceNotFoundException;
import com.taskflow.common.api.PageResponse;
import com.taskflow.task.infrastructure.JdbcTaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    private final JdbcCommentRepository commentRepository;
    private final JdbcTaskRepository taskRepository;

    public CommentService(JdbcCommentRepository commentRepository,
                          JdbcTaskRepository taskRepository) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
    }

    public CommentResponse createComment(long taskId, CreateCommentRequest req) {
        requireTaskExists(taskId);
        Comment saved = commentRepository.save(taskId, req.authorId(), req.body().strip());
        return toResponse(saved);
    }

    public PageResponse<CommentResponse> listComments(long taskId, int page, int size) {
        requireTaskExists(taskId);
        long total = commentRepository.countByTaskId(taskId);
        List<CommentResponse> content = commentRepository
                .findByTaskId(taskId, size, page * size)
                .stream().map(this::toResponse).toList();
        return PageResponse.of(content, page, size, total);
    }

    private void requireTaskExists(long taskId) {
        taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task " + taskId + " not found"));
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
