package com.culciful.security;

import com.culciful.config.RequestSizeLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

/**
 * 全局请求体大小闸：按 Content-Length 头拒掉超大的非 multipart 请求，
 * 在鉴权 / 反序列化之前就返回，省掉无谓开销。
 * multipart 由 {@link com.culciful.config.MultipartConfiguration} 单独限制，这里放行。
 * 注：分块传输（无 Content-Length）此处放行，依赖容器与 @Size 校验兜底。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final RequestSizeLimitProperties properties;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String contentType = request.getContentType();
        boolean multipart = contentType != null
                && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/");
        long declaredLength = request.getContentLengthLong();

        if (!multipart && declaredLength > properties.getMaxBodySize().toBytes()) {
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"errorCode\":-10012,\"message\":\"request body too large\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
