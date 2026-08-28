package com.culciful.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.culciful.common.api.R;
import com.culciful.common.enums.ResultCodeEnum;
import com.culciful.dto.EmailExistParam;
import com.culciful.dto.RegisterRequest;
import com.culciful.mapper.UserInfoMapper;
import com.culciful.pojo.UserInfo;
import com.culciful.service.EmailVerificationCodeService;
import com.culciful.service.UserService;
import com.culciful.utils.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserInfoMapper userInfoMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationCodeService emailVerificationCodeService;

    @Override
    @Transactional
    public R<Void> register(RegisterRequest request) {
        if (!emailVerificationCodeService.consumeCode(
                request.email(),
                request.verificationCode(),
                EmailVerificationCodeService.SCENE_REGISTER)) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        boolean emailExists = userInfoMapper.selectCount(new LambdaQueryWrapper<UserInfo>()
                .eq(UserInfo::getIsDeleted, false)
                .eq(UserInfo::getEmail, request.email())) > 0;
        if (emailExists) {
            return R.fail(ResultCodeEnum.EMAIL_USED);
        }

        boolean usernameExists = userInfoMapper.selectCount(new LambdaQueryWrapper<UserInfo>()
                .eq(UserInfo::getIsDeleted, false)
                .eq(UserInfo::getUsername, request.username())) > 0;
        if (usernameExists) {
            return R.fail(ResultCodeEnum.USERNAME_USED);
        }

        UserInfo user = new UserInfo();
        long id = snowflakeIdGenerator.nextId();
        user.setId(id);
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setIsDeleted(false);
        user.setDeletedToken(0L);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        try {
            userInfoMapper.insert(user);
        } catch (DataIntegrityViolationException ex) {
            String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (msg.contains("uk_user_email") || msg.contains("email")) {
                return R.fail(ResultCodeEnum.EMAIL_USED);
            }
            if (msg.contains("uk_user_username") || msg.contains("username")) {
                return R.fail(ResultCodeEnum.USERNAME_USED);
            }
            throw ex;
        }
        return R.ok(null);
    }

    @Override
    public R<Map<String, Boolean>> checkEmailExist(EmailExistParam emailExistParam) {
        Long count = userInfoMapper.selectCount(new LambdaQueryWrapper<UserInfo>()
                .eq(UserInfo::getEmail, emailExistParam.email())
                .eq(UserInfo::getIsDeleted, false));
        Map<String, Boolean> data = new HashMap<>();
        data.put("isExisted", count > 0);
        return R.ok(data);
    }
}
