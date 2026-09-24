package com.culciful.security.token;

import com.alibaba.druid.util.StringUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Data
@Component
@ConfigurationProperties(prefix = "jwt.token")
public class JwtHelper {

    /** 有效期（分钟） */
    private long tokenExpiration;
    /** HS512 签名密钥（配置串本身，>= 64 字符） */
    private String tokenSignKey;

    /** 签名用的 Key 对象，validate() 里从 tokenSignKey 派生一次，避免每次签名/验签都重建 */
    private SecretKey signingKey;

    @PostConstruct
    void validate() {
        if (tokenSignKey == null || tokenSignKey.length() < 64) {
            throw new IllegalStateException(
                    "jwt.token.tokenSignKey 未配置或长度不足（需 >= 64 字符）。生产请注入 JWT_TOKEN_SIGN_KEY 环境变量。");
        }
        if (tokenExpiration <= 0) {
            throw new IllegalStateException("jwt.token.tokenExpiration 必须为正数（分钟）");
        }
        // 配置串的 UTF-8 字节直接当密钥材料（不是 base64 解码）：上面的长度校验保证
        // >= 64 字节，满足 HS512 的最小密钥长度要求，语义上和长度校验保持一致。
        signingKey = Keys.hmacShaKeyFor(tokenSignKey.getBytes(StandardCharsets.UTF_8));
    }

    public long cookieMaxAgeSeconds() {
        return tokenExpiration * 60L;
    }

    private long ttlMillis() {
        return tokenExpiration * 60L * 1000L;
    }

    /** 剩余寿命低于总时长的一半时，滑动续期 */
    public boolean shouldRenew(String token) {
        long remaining = remainingMillis(token);
        return remaining > 0 && remaining < ttlMillis() / 2;
    }

    public String createToken(Long userId, long tokenVersion) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject("blog-user")
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMillis()))
                .claim("userId", userId)
                .claim("tv", tokenVersion)
                .signWith(signingKey)
                .compressWith(Jwts.ZIP.GZIP)
                .compact();
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    public Long getUserId(String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        Object uid = parse(token).get("userId");
        if (uid == null) {
            return null;
        }
        return uid instanceof Number ? ((Number) uid).longValue() : Long.parseLong(uid.toString());
    }

    /** token 里的版本号；无此 claim 视为 0（兼容旧 token） */
    public long getTokenVersion(String token) {
        if (StringUtils.isEmpty(token)) {
            return 0L;
        }
        Object tv = parse(token).get("tv");
        return tv instanceof Number ? ((Number) tv).longValue() : 0L;
    }

    public long remainingMillis(String token) {
        try {
            return parse(token).getExpiration().getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            return -1L;
        }
    }

    public boolean isExpired(String token) {
        try {
            return parse(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
