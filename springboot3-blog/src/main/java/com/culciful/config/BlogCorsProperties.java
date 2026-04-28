package com.culciful.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-origin allowlist. Dev: http://localhost:5173. Prod same-site: leave empty.
 */
@Data
@Component
@ConfigurationProperties(prefix = "blog.cors")
public class BlogCorsProperties {

    private List<String> allowedOrigins = new ArrayList<>();
}
