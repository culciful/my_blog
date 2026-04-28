package com.culciful.security;

import com.culciful.config.JwtCookieProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/**
 * Issue / clear JWT using HttpOnly cookie (scheme C).
 */
@Service
@RequiredArgsConstructor
public class JwtCookieService {

    private final JwtCookieProperties cookieProperties;

    public void addTokenCookie(HttpServletResponse response, String jwt, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(cookieProperties.getName(), jwt)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .path(cookieProperties.getPath())
                .maxAge(maxAgeSeconds)
                .sameSite(cookieProperties.getSameSite())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieProperties.getName(), "")
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .path(cookieProperties.getPath())
                .maxAge(0)
                .sameSite(cookieProperties.getSameSite())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
