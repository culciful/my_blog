package com.culciful.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.culciful.dto.LoginRequest;
import com.culciful.mapper.UserInfoMapper;
import com.culciful.pojo.UserInfo;
import com.culciful.security.JwtCookieService;
import com.culciful.security.LoginAttemptService;
import com.culciful.service.UserAuthService;
import com.culciful.security.token.JwtHelper;
import com.culciful.common.api.R;
import com.culciful.common.enums.ResultCodeEnum;
import com.culciful.utils.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private final UserInfoMapper userInfoMapper;
    private final JwtHelper jwtHelper;
    private final JwtCookieService jwtCookieService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    @Override
    public R<Map<String, Object>> login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        String attemptKey = loginAttemptService.key(request.username(), RequestUtils.clientIp(httpRequest));
        if (loginAttemptService.isBlocked(attemptKey)) {
            return R.fail(ResultCodeEnum.LOGIN_LOCKED);
        }
        UserInfo user = authenticate(request.username(), request.password());
        if (user == null) {
            loginAttemptService.recordFailure(attemptKey);
            return R.fail(ResultCodeEnum.LOGIN_FAILED);
        }
        loginAttemptService.reset(attemptKey);
        long tv = user.getTokenVersion() == null ? 0L : user.getTokenVersion();
        String jwt = jwtHelper.createToken(user.getId(), tv);
        jwtCookieService.addTokenCookie(response, jwt, jwtHelper.cookieMaxAgeSeconds());
        return R.ok(toProfileMap(user, true));
    }

    @Override
    public UserInfo authenticate(String usernameOrEmail, String rawPassword) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank() || rawPassword == null) {
            return null;
        }
        // 含 @ 按邮箱匹配，否则按用户名匹配——避免「用户名」与「他人邮箱」字面相同导致的歧义
        boolean asEmail = usernameOrEmail.contains("@");
        UserInfo user = userInfoMapper.selectOne(new LambdaQueryWrapper<UserInfo>()
                .eq(UserInfo::getIsDeleted, false)
                .eq(asEmail ? UserInfo::getEmail : UserInfo::getUsername, usernameOrEmail)
                .last("LIMIT 1"));
        if (user == null) {
            return null;
        }
        if (user.getPassword() == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            return null;
        }
        return user;
    }

    private static Map<String, Object> toProfileMap(UserInfo user, boolean includeEmail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("avatarUrl", null);
        if (user.getCreatedAt() != null) {
            m.put("createTime", user.getCreatedAt().atZone(ZoneId.systemDefault()).toEpochSecond());
        } else {
            m.put("createTime", 0L);
        }
        if (includeEmail) {
            m.put("email", user.getEmail());
        }
        return m;
    }
}
