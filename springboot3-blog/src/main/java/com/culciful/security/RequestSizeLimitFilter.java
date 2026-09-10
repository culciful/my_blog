package com.culciful.security;

import com.culciful.config.RequestSizeLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 全局请求体大小闸（非 multipart）：
 * <ol>
 *   <li>Content-Length 已超标 → 不读 body，直接 413，省掉无谓开销</li>
 *   <li>否则把 body 流包一层硬上限，读到超过上限（分块传输谎报 Content-Length 也拦得住）就抛异常</li>
 * </ol>
 * multipart 由 {@link com.culciful.config.MultipartConfiguration} 单独限制，这里放行。
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

        if (multipart) {
            filterChain.doFilter(request, response);
            return;
        }

        long maxBytes = properties.getMaxBodySize().toBytes();
        if (request.getContentLengthLong() > maxBytes) {
            writeTooLarge(response);
            return;
        }
        filterChain.doFilter(new LimitedBodyRequest(request, maxBytes), response);
    }

    private static void writeTooLarge(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"errorCode\":-10012,\"message\":\"request body too large\"}");
    }

    /** 读到超过上限就抛 IllegalArgumentException（→ GlobalExceptionHandler → -10012）。 */
    static final class TooLargeException extends IllegalArgumentException {
        TooLargeException() {
            super("request body too large");
        }
    }

    private static final class LimitedBodyRequest extends HttpServletRequestWrapper {
        private final long limit;

        LimitedBodyRequest(HttpServletRequest request, long limit) {
            super(request);
            this.limit = limit;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new LimitedServletInputStream(super.getInputStream(), limit);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String enc = getCharacterEncoding();
            return new BufferedReader(new InputStreamReader(getInputStream(),
                    enc != null ? enc : StandardCharsets.UTF_8.name()));
        }
    }

    private static final class LimitedServletInputStream extends ServletInputStream {
        private final ServletInputStream delegate;
        private final long limit;
        private long count;

        LimitedServletInputStream(ServletInputStream delegate, long limit) {
            this.delegate = delegate;
            this.limit = limit;
        }

        private void tally(int read) {
            if (read > 0) {
                count += read;
                if (count > limit) {
                    throw new TooLargeException();
                }
            }
        }

        @Override
        public int read() throws IOException {
            int b = delegate.read();
            if (b != -1) {
                tally(1);
            }
            return b;
        }

        @Override
        public int read(@NonNull byte[] b, int off, int len) throws IOException {
            int n = delegate.read(b, off, len);
            tally(n);
            return n;
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }

        @Override
        public int available() throws IOException {
            return delegate.available();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
