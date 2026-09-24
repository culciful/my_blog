package com.culciful.utils;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestUtils {

    private RequestUtils() {
    }

    /**
     * 客户端 IP，用于限流。
     *
     * <p>只取 {@code getRemoteAddr()}（直连对端），<b>不解析 X-Forwarded-For / X-Real-IP</b> ——
     * 那些头可被客户端随意伪造，直接信任等于让「按 IP 限流」形同虚设。
     * 部署在 Nginx 等反代后面时，配置 {@code server.forward-headers-strategy=framework}
     * （或 {@code native}），Spring / 容器会校验并处理这些头，{@code getRemoteAddr()}
     * 返回的就是真实客户端 IP。</p>
     */
    public static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "" : remote;
    }
}
