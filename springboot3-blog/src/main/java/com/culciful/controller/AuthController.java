package com.culciful.controller;

import com.culciful.dto.LoginRequest;
import com.culciful.service.UserAuthService;
import com.culciful.security.JwtCookieService;
import com.culciful.common.api.R;
import com.culciful.security.crypto.RSAUtil;
import com.culciful.common.enums.ResultCodeEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
    private final ObjectMapper objectMapper;

    /**
     * Body: RSA public key encrypted JSON payload from frontend JSEncrypt.
     * Content-Type: text/plain
     */
    @PostMapping(value = "/login", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public R<Map<String, Object>> login(@RequestBody String cipherBody, HttpServletResponse response) {
        try {
            String plain = RSAUtil.decryptWithPrivate(cipherBody);
            LoginRequest request = objectMapper.readValue(plain, LoginRequest.class);
            return userAuthService.login(request, response);
        } catch (Exception e) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletResponse response) {
        jwtCookieService.clearTokenCookie(response);
        return R.ok(null);
    }
}
