package com.culciful.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮箱验证码配置。`blog.email-code.*` 可覆盖。
 */
@Data
@Component
@ConfigurationProperties(prefix = "blog.email-code")
public class EmailCodeProperties {

    /** 验证码有效期（分钟） */
    private int expireMinutes = 10;

    /** 同邮箱 + 场景的重发冷却（秒） */
    private int resendCooldownSeconds = 60;

    /** 一个验证码最多允许校验失败次数，超过即作废（需重新获取） */
    private int maxVerifyAttempts = 5;

    /** 同邮箱每日最多发送次数 */
    private int perEmailDailyLimit = 10;

    /** 同 IP 每小时最多发送次数 */
    private int perIpHourlyLimit = 10;

    /** 同 IP 每日最多发送次数 */
    private int perIpDailyLimit = 30;

    /** 发信人地址（未配置 spring.mail 时无意义） */
    private String from = "no-reply@culciful.blog";
}
