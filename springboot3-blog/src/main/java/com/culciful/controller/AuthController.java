package com.culciful.controller;

import com.culciful.dto.LoginRequest;
import com.culciful.service.UserAuthService;
import com.culciful.security.JwtCookieService;
import com.culciful.common.api.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserAuthService userAuthService;
    private final JwtCookieService jwtCookieService;

    /** 明文 JSON；传输安全依赖 HTTPS（生产硬性要求） */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody @Valid LoginRequest request,
                                        HttpServletRequest httpRequest,
                                        HttpServletResponse response) {
        return userAuthService.login(request, httpRequest, response);
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletResponse response) {
        jwtCookieService.clearTokenCookie(response);
        return R.ok(null);
    }
}
