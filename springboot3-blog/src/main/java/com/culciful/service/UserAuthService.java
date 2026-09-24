package com.culciful.service;

import com.culciful.dto.LoginRequest;
import com.culciful.pojo.UserInfo;
import com.culciful.common.api.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

public interface UserAuthService {

    R<Map<String, Object>> login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response);

    /**
     * @return null if not found or password mismatch
     */
    UserInfo authenticate(String usernameOrEmail, String rawPassword);
}
