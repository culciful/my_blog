package com.culciful.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 登录失败限流配置：按 用户名+IP 计数，超过阈值锁定一段时间。
 * 内存实现（Caffeine），单实例够用；多实例部署需换成 Redis。
 */
@Data
@Component
@ConfigurationProperties(prefix = "blog.login-rate-limit")
public class LoginRateLimitProperties {

    /** 关闭后完全不计数、不拦截 */
    private boolean enabled = true;

    /** 连续失败达到该次数后锁定 */
    private int maxAttempts = 5;

    /** 锁定时长（最后一次失败起算），到期后计数清零 */
    private Duration lockDuration = Duration.ofMinutes(15);
}
