package com.culciful.security;

import com.culciful.config.LoginRateLimitProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

/**
 * 登录失败计数器。key = 规范化用户名 + "|" + 客户端 IP。
 * 达到阈值后 {@link #isBlocked} 返回 true；成功登录调用 {@link #reset} 清零。
 * 条目在最后一次失败后 lockDuration 到期自动清除。
 */
@Service
public class LoginAttemptService {

    private final LoginRateLimitProperties properties;
    private final Cache<String, Integer> failures;

    public LoginAttemptService(LoginRateLimitProperties properties) {
        this.properties = properties;
        this.failures = Caffeine.newBuilder()
                .expireAfterWrite(properties.getLockDuration())
                .maximumSize(100_000)
                .build();
    }

    public String key(String usernameOrEmail, String clientIp) {
        String u = usernameOrEmail == null ? "" : usernameOrEmail.trim().toLowerCase();
        return u + "|" + (clientIp == null ? "" : clientIp);
    }

    public boolean isBlocked(String key) {
        if (!properties.isEnabled()) {
            return false;
        }
        Integer count = failures.getIfPresent(key);
        return count != null && count >= properties.getMaxAttempts();
    }

    public void recordFailure(String key) {
        if (!properties.isEnabled()) {
            return;
        }
        failures.asMap().merge(key, 1, Integer::sum);
    }

    public void reset(String key) {
        failures.invalidate(key);
    }
}
