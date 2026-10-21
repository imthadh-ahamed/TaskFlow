package com.taskflow.common.api;

import java.util.List;

/**
 * Stable error envelope — every error response uses this shape.
 * Fields must remain compatible across releases; add fields, never remove.
 */
public record ErrorResponse(
        int status,
        String error,
        String code,
        String message,
        List<FieldError> details,
        String timestamp,
        String path
) {

    public record FieldError(String field, String message) {}
}
