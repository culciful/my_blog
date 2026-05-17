package com.culciful.security.token;

import com.alibaba.druid.util.StringUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.CompressionCodecs;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Date;

@Data
@Component
@ConfigurationProperties(prefix = "jwt.token")
public class JwtHelper {

    /** Token lifetime in minutes, consistent with the existing configuration. */
    private long tokenExpiration;
    private String tokenSignKey;

    public long cookieMaxAgeSeconds() {
        return tokenExpiration * 60L;
    }

    public String createToken(Long userId) {
        return Jwts.builder()
                .setSubject("blog-user")
                .setExpiration(new Date(System.currentTimeMillis() + tokenExpiration * 1000L * 60L))
                .claim("userId", userId)
                .signWith(SignatureAlgorithm.HS512, tokenSignKey)
                .compressWith(CompressionCodecs.GZIP)
                .compact();
    }

    public Long getUserId(String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        Jws<Claims> claimsJws = Jwts.parser().setSigningKey(tokenSignKey).parseClaimsJws(token);
        Claims claims = claimsJws.getBody();
        Object uid = claims.get("userId");
        if (uid == null) {
            return null;
        }
        if (uid instanceof Number) {
            return ((Number) uid).longValue();
        }
        return Long.parseLong(uid.toString());
    }

    public boolean isExpiration(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(tokenSignKey)
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration()
                    .before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
