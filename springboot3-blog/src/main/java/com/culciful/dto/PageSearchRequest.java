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

    /**
     * 搜索关键词：trim + 截断到 64。filter 是弱类型 Map，前端 maxlength 也可能被绕过，
     * 这里统一兜住长度（关键词只做 LIKE 绑定参数，无注入风险，但没必要放行超长串）。
     */
    public String safeKeyword() {
        Object keyword = safeFilter().get("keyword");
        if (keyword == null) {
            return "";
        }
        String trimmed = keyword.toString().trim();
        return trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
    }
}
