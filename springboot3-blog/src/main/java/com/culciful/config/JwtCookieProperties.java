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

    /** Lax is typical for same-site / SPA */
    private String sameSite = "Lax";
}
