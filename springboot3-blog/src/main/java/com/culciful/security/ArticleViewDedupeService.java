package com.culciful.security;

import com.culciful.config.ArticleViewProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

/**
 * 浏览量去重：key = 文章ID + "|" + 客户端IP。{@link #shouldCount} 在去重窗口内对同一
 * key 第一次调用返回 true（顺带记一条），窗口内后续调用都返回 false；条目到期自动清除
 * （Caffeine {@code expireAfterWrite}，不用另外写清理逻辑）。
 *
 * <p>用 {@code Map.putIfAbsent} 而不是"先 get 再 put"：并发下同一 IP 几乎同时点开同一篇
 * 文章会有多个请求同时判断，先 get 再 put 会出现多个线程都读到"没记录"从而重复计数；
 * putIfAbsent 是原子操作，只有第一个到达的线程能拿到 null 返回值。</p>
 */
@Service
public class ArticleViewDedupeService {

    private final ArticleViewProperties properties;
    private final Cache<String, Boolean> recentViews;

    public ArticleViewDedupeService(ArticleViewProperties properties) {
        this.properties = properties;
        this.recentViews = Caffeine.newBuilder()
                .expireAfterWrite(properties.getDedupeWindow())
                .maximumSize(1_000_000)
                .build();
    }

    public boolean shouldCount(Long articleId, String clientIp) {
        if (!properties.isDedupeEnabled()) {
            return true;
        }
        String key = articleId + "|" + (clientIp == null ? "" : clientIp);
        return recentViews.asMap().putIfAbsent(key, Boolean.TRUE) == null;
    }
}
