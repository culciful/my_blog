package com.culciful.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 文章浏览量防刷：同一 IP 对同一篇文章，去重窗口内只计一次。
 * 内存实现（Caffeine），单实例够用；多实例部署需换成 Redis。
 */
@Data
@Component
@ConfigurationProperties(prefix = "blog.article-view")
public class ArticleViewProperties {

    /** 关闭后完全不去重，每次访问都计数（等同之前的行为） */
    private boolean dedupeEnabled = true;

    /** 去重窗口：同一 IP+文章 在这段时间内重复访问不重复计数 */
    private Duration dedupeWindow = Duration.ofMinutes(10);
}
