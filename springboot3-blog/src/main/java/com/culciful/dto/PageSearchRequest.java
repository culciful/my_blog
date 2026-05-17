package com.culciful.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.Collections;
import java.util.Map;

public record PageSearchRequest(
        @Min(1)
        @Max(100)
        Integer pageSize,

        @Min(1)
        Integer currentPage,

        Map<String, Object> filter
) {
    public int safePageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    public int safeCurrentPage() {
        if (currentPage == null || currentPage < 1) {
            return 1;
        }
        return currentPage;
    }

    public Map<String, Object> safeFilter() {
        return filter == null ? Collections.emptyMap() : filter;
    }
}
