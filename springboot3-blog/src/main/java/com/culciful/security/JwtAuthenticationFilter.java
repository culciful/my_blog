package com.culciful.security;

import com.culciful.config.JwtCookieProperties;
import com.culciful.mapper.UserInfoMapper;
import com.culciful.pojo.UserInfo;
import com.culciful.security.token.JwtHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 从 HttpOnly cookie 读 JWT，校验签名 / 有效期 / token_version，通过则写入 SecurityContext。
 * 剩余寿命不足一半时滑动续期（重新下发 cookie），活跃用户不会因超时掉线。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtHelper jwtHelper;
    private final JwtCookieProperties cookieProperties;
    private final JwtCookieService jwtCookieService;
    private final UserInfoMapper userInfoMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = readTokenFromCookie(request);
            if (token != null && !jwtHelper.isExpired(token)) {
                Long userId = jwtHelper.getUserId(token);
                UserInfo user = userId == null ? null : userInfoMapper.selectById(userId);
                if (user != null && tokenVersionMatches(user, token)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userId.toString(),
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    if (jwtHelper.shouldRenew(token)) {
                        String fresh = jwtHelper.createToken(userId, versionOf(user));
                        jwtCookieService.addTokenCookie(response, fresh, jwtHelper.cookieMaxAgeSeconds());
                    }
                }
            }
        } catch (Exception ignored) {
            // 无效 cookie：保持未认证，不清空已有 context
        }
        filterChain.doFilter(request, response);
    }

    private boolean tokenVersionMatches(UserInfo user, String token) {
        return versionOf(user) == jwtHelper.getTokenVersion(token);
    }

    private long versionOf(UserInfo user) {
        return user.getTokenVersion() == null ? 0L : user.getTokenVersion();
    }

    private String readTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (cookieProperties.getName().equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
