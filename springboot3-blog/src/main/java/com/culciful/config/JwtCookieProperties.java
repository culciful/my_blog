package com.culciful.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt.cookie")
public class JwtCookieProperties {

    /** HttpOnly cookie name holding the JWT */
    private String name = "BLOG_TOKEN";

    /** Cookie path; use / so all API paths receive the token */
    private String path = "/";

    /** true in production (HTTPS); false for local http */
    private boolean secure = false;

    /**
     * Strict：跨站请求（含外链点进来的首次导航）一律不带这张 cookie。SPA 的 HTML 外壳本来就不需要
     * 鉴权，页面加载完之后同域发起的 XHR/fetch 仍然算 same-site、正常带 cookie——所以从别处点文章
     * 链接进来不受影响，只是那一次"整页导航"请求本身拿不到 cookie（反正它也用不上）。
     * 全站写操作走 POST（RPC 风格，见 BlogCorsProperties 的注释），Lax 对跨站 POST 本来就不带 cookie，
     * 换成 Strict 唯一多挡的是跨站 GET 顶层导航——这类端点本项目里也都不做状态变更，收益不大但也零成本，
     * 属于纯粹加固、没有已知的外链场景会被破坏。
     */
    private String sameSite = "Strict";
}
