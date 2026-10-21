package com.taskflow.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Stable error envelope — every error response uses this shape.
 * Fields must remain compatible across releases; add fields, never remove.
 */
@Schema(description = "Standard error response returned for all non-2xx responses")
public record ErrorResponse(
        @Schema(description = "HTTP status code", example = "400")
        int status,
        @Schema(description = "Short HTTP reason phrase", example = "Validation failed")
        String error,
        @Schema(description = "Machine-readable error code; stable across releases", example = "VALIDATION_ERROR")
        String code,
        @Schema(description = "Human-readable description", example = "Validation failed")
        String message,
        @Schema(description = "Per-field validation errors; empty for non-validation errors")
        List<FieldError> details,
        @Schema(description = "ISO 8601 UTC timestamp", example = "2026-10-21T10:00:00Z")
        String timestamp,
        @Schema(description = "Request URI", example = "/api/v1/projects")
        String path
) {

    @Schema(name = "FieldError", description = "A single field-level validation error")
    public record FieldError(
            @Schema(description = "Field name", example = "title")
            String field,
            @Schema(description = "Validation message", example = "must not be blank")
            String message
    ) {}
}
