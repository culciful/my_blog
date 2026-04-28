package com.culciful.controller;

import com.culciful.mapper.UserInfoMapper;
import com.culciful.param.EmailExistParam;
import com.culciful.pojo.UserInfo;
import com.culciful.service.UserInfoService;
import com.culciful.common.api.R;
import com.culciful.common.enums.ResultCodeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
/**
 * @author culciful_zy
 * @version 1.0
 * date 2024/1/31 15:25
 * description:
 */

@RestController
@RequestMapping("user")
public class UserController {
    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private UserInfoMapper userInfoMapper;

    // @PostMapping("login")
    // public Result login(@RequestBody String requestBody){
    //     // Result result = userInfoService.login(user);
    //     System.out.println("requestBody = " + requestBody);
    //     return null;
    // }
    //
    // @PostMapping("register")
    // public Result register(@RequestBody UserInfo userInfo){
    //     // Result result = userInfoService.login(user);
    //     System.out.println("requestBody = " + userInfo);
    //     userInfoService.saveUserDetails(userInfo);
    //     return null;
    // }

    /**
     * Check whether email already exists.
     */
    @PostMapping("checkEmailExist")
    public R checkEmailExist(@RequestBody @Validated EmailExistParam emailExistParam, BindingResult result) {
        // validate request body
        boolean b = result.hasErrors();
        if(b) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        R r = userInfoService.checkEmailExist(emailExistParam);
        return r;
    }

    /**
     * Public profile query by id.
     */
    @GetMapping("getUserInfo")
    public R<Map<String, Object>> getUserPublicProfile(@RequestParam(value = "id", required = false) String idParam) {
        Long targetId = parseId(idParam);
        if (targetId == null) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        UserInfo user = loadActiveUser(targetId);
        if (user == null) {
            return R.fail(ResultCodeEnum.USERNAME_ERROR);
        }
        return R.ok(toPublicProfile(user));
    }

    /**
     * Current logged-in user profile, requires JWT.
     */
    @GetMapping("getMyProfile")
    public R<Map<String, Object>> getMyProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return R.fail(ResultCodeEnum.NOT_LOGIN);
        }
        long selfId = Long.parseLong(auth.getName());
        UserInfo user = loadActiveUser(selfId);
        if (user == null) {
            return R.fail(ResultCodeEnum.USERNAME_ERROR);
        }
        Map<String, Object> m = toPublicProfile(user);
        m.put("email", user.getEmail());
        return R.ok(m);
    }

    /** REST alias: {@code GET /user/users/me}. */
    @GetMapping("users/me")
    public R<Map<String, Object>> getMyProfileRest() {
        return getMyProfile();
    }

    /** REST alias: {@code GET /user/users/{id}}. */
    @GetMapping("users/{id:\\d+}")
    public R<Map<String, Object>> getUserPublicProfileByPath(@PathVariable("id") String idParam) {
        return getUserPublicProfile(idParam);
    }

    /** REST alias: {@code POST /user/users/email-existence}. */
    @PostMapping("users/email-existence")
    public R checkEmailExistRest(@RequestBody @Validated EmailExistParam emailExistParam, BindingResult result) {
        return checkEmailExist(emailExistParam, result);
    }

    private UserInfo loadActiveUser(long id) {
        UserInfo user = userInfoMapper.selectById((int) id);
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() != 0)) {
            return null;
        }
        return user;
    }

    private static Map<String, Object> toPublicProfile(UserInfo user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("avatarUrl", user.getAvatarUrl());
        m.put("createTime", user.getCreateTime() != null ? user.getCreateTime().getTime() / 1000L : 0L);
        return m;
    }

    private static Long parseId(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        String s = raw.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        return Long.parseLong(s);
    }
}
