package com.culciful.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 跨域配置。三项都可被环境变量 / profile 覆盖，默认值贴合本项目实际用法。
 * <ul>
 *   <li>dev：{@code allowed-origins} = http://localhost:5173（见 application-dev.yaml）</li>
 *   <li>生产同站部署（SPA 与 API 同源）：留空，{@link com.culciful.config.SecurityConfiguration}
 *       检测到空列表就完全不注册 CORS 过滤器</li>
 *   <li>生产跨站部署：注入 {@code BLOG_PUBLIC_ORIGIN=https://your-domain}（多个逗号分隔）</li>
 * </ul>
 */
@Data
@Component
@ConfigurationProperties(prefix = "blog.cors")
public class BlogCorsProperties {

    private List<String> allowedOrigins = new ArrayList<>();

    /**
     * 允许的请求头。前端只发 {@code Content-Type}（JSON / multipart）；
     * JWT 走 HttpOnly cookie，没有 Authorization 头。要加自定义头在这里显式列出。
     */
    private List<String> allowedHeaders = new ArrayList<>(List.of("Content-Type"));

    /** 允许的方法。本 API 只有 GET / POST（RPC 风格，标识放 body），没有 PUT/PATCH/DELETE。 */
    private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "OPTIONS"));

    /**
     * 去掉空白来源：生产 {@code ${BLOG_PUBLIC_ORIGIN:}} 未注入时按 profile 可能绑成 {@code [""]}，
     * 空串会被 Spring 当成一个非法 origin。过滤后为空 = 同站部署，不启用 CORS。
     */
    public List<String> resolvedAllowedOrigins() {
        return allowedOrigins.stream()
                .filter(o -> o != null && !o.isBlank())
                .map(String::trim)
                .toList();
    }
}
