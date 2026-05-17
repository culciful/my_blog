package com.culciful.controller;

import com.culciful.dto.LoginRequest;
import com.culciful.service.UserAuthService;
import com.culciful.security.JwtCookieService;
import com.culciful.common.api.R;
import com.culciful.security.crypto.EncryptedBody;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserAuthService userAuthService;
    private final JwtCookieService jwtCookieService;

    /**
     * Body: RSA public key encrypted JSON payload from frontend JSEncrypt.
     * Content-Type: text/plain
     */
    @PostMapping(value = "/login", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public R<Map<String, Object>> login(@EncryptedBody @Valid LoginRequest request, HttpServletResponse response) {
        return userAuthService.login(request, response);
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletResponse response) {
        jwtCookieService.clearTokenCookie(response);
        return R.ok(null);
    }
}
