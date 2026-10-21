package com.taskflow.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated response wrapper. `content` holds the items; "
        + "`total` is the full count across all pages.")
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long total,
        int totalPages
) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long total) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, totalPages);
    }
}
