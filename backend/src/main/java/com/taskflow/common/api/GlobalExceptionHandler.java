package com.taskflow.common.api;

import com.taskflow.common.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(
                400, "Validation failed", "VALIDATION_ERROR",
                "Validation failed", details, now(), req.getRequestURI()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest req) {
        var details = ex.getConstraintViolations().stream()
                .map(cv -> {
                    String field = cv.getPropertyPath().toString();
                    // strip method prefix (e.g. "listProjects.size" → "size")
                    int dot = field.lastIndexOf('.');
                    return new ErrorResponse.FieldError(
                            dot >= 0 ? field.substring(dot + 1) : field,
                            cv.getMessage());
                })
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(
                400, "Validation failed", "VALIDATION_ERROR",
                "Validation failed", details, now(), req.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
                400, "Bad Request", "INVALID_REQUEST",
                ex.getMessage(), List.of(), now(), req.getRequestURI()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                404, "Not Found", "NOT_FOUND",
                ex.getMessage(), List.of(), now(), req.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest req) {
        // Do not expose constraint names or SQL details
        return ResponseEntity.status(422).body(new ErrorResponse(
                422, "Unprocessable Entity", "DATA_INTEGRITY_ERROR",
                "Request violates a data constraint — verify referenced resources exist",
                List.of(), now(), req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
                500, "Internal Server Error", "INTERNAL_ERROR",
                "An unexpected error occurred", List.of(), now(), req.getRequestURI()));
    }

    private static String now() {
        return Instant.now().toString();
    }
}
