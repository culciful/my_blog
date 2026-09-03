package com.culciful.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.culciful.mapper.EmailVerificationCodeMapper;
import com.culciful.mapper.UserInfoMapper;
import com.culciful.pojo.EmailVerificationCode;
import com.culciful.pojo.UserInfo;
import com.culciful.service.EmailVerificationCodeService;
import com.culciful.utils.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationCodeServiceImpl implements EmailVerificationCodeService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int EXPIRE_MINUTES = 10;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final EmailVerificationCodeMapper emailVerificationCodeMapper;
    private final UserInfoMapper userInfoMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String sendCode(String email, String scene) {
        String normalizedScene = normalizeScene(scene);
        boolean emailExists = userInfoMapper.selectCount(new LambdaQueryWrapper<UserInfo>()
                .eq(UserInfo::getEmail, email)
                .eq(UserInfo::getIsDeleted, false)) > 0;
        // register / update_email：目标邮箱不能已被占用；reset：目标邮箱必须存在
        if ((SCENE_REGISTER.equals(normalizedScene) || SCENE_UPDATE_EMAIL.equals(normalizedScene)) && emailExists) {
            return "EMAIL_USED";
        }
        if (SCENE_RESET_PASSWORD.equals(normalizedScene) && !emailExists) {
            return "EMAIL_NOT_FOUND";
        }
        EmailVerificationCode latest = emailVerificationCodeMapper.selectOne(new LambdaQueryWrapper<EmailVerificationCode>()
                .eq(EmailVerificationCode::getEmail, email)
                .eq(EmailVerificationCode::getScene, normalizedScene)
                .orderByDesc(EmailVerificationCode::getCreatedAt)
                .last("LIMIT 1"));
        if (latest != null && latest.getCreatedAt() != null
                && latest.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
            return "RATE_LIMIT";
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        EmailVerificationCode entity = new EmailVerificationCode();
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setEmail(email);
        entity.setScene(normalizedScene);
        entity.setCodeHash(passwordEncoder.encode(code));
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRE_MINUTES));
        entity.setCreatedAt(LocalDateTime.now());
        emailVerificationCodeMapper.insert(entity);
        System.out.println("dev email verification code for " + email + " [" + normalizedScene + "]: " + code);
        return null;
    }

    @Override
    public boolean verifyCode(String email, String code, String scene) {
        EmailVerificationCode entity = latestValid(email, scene);
        return entity != null && passwordEncoder.matches(code, entity.getCodeHash());
    }

    @Override
    public boolean consumeCode(String email, String code, String scene) {
        EmailVerificationCode entity = latestValid(email, scene);
        if (entity == null || !passwordEncoder.matches(code, entity.getCodeHash())) {
            return false;
        }
        entity.setUsedAt(LocalDateTime.now());
        emailVerificationCodeMapper.updateById(entity);
        return true;
    }

    private EmailVerificationCode latestValid(String email, String scene) {
        if (email == null || email.isBlank() || scene == null || scene.isBlank()) {
            return null;
        }
        return emailVerificationCodeMapper.selectOne(new LambdaQueryWrapper<EmailVerificationCode>()
                .eq(EmailVerificationCode::getEmail, email)
                .eq(EmailVerificationCode::getScene, normalizeScene(scene))
                .isNull(EmailVerificationCode::getUsedAt)
                .gt(EmailVerificationCode::getExpiresAt, LocalDateTime.now())
                .orderByDesc(EmailVerificationCode::getCreatedAt)
                .last("LIMIT 1"));
    }

    private String normalizeScene(String scene) {
        return scene == null || scene.isBlank() ? SCENE_REGISTER : scene.trim();
    }
}
