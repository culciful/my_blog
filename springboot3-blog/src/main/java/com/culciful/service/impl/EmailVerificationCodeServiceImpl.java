package com.culciful.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.culciful.config.EmailCodeProperties;
import com.culciful.mapper.EmailVerificationCodeMapper;
import com.culciful.mapper.UserInfoMapper;
import com.culciful.pojo.EmailVerificationCode;
import com.culciful.pojo.UserInfo;
import com.culciful.service.EmailVerificationCodeService;
import com.culciful.service.VerificationCodeMailer;
import com.culciful.utils.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationCodeServiceImpl implements EmailVerificationCodeService {

    static final String RATE_LIMIT = "RATE_LIMIT";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationCodeMapper emailVerificationCodeMapper;
    private final UserInfoMapper userInfoMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final PasswordEncoder passwordEncoder;
    private final EmailCodeProperties properties;
    private final VerificationCodeMailer mailer;

    @Override
    public String sendCode(String email, String scene, String requestIp) {
        String normalizedScene = normalizeScene(scene);
        boolean isEmailRegistered = userInfoMapper.selectCount(new LambdaQueryWrapper<UserInfo>()
                .eq(UserInfo::getEmail, email)
                .eq(UserInfo::getIsDeleted, false)) > 0;
        // register / update_email：目标邮箱不能已被占用；reset：目标邮箱必须存在
        if ((SCENE_REGISTER.equals(normalizedScene) || SCENE_UPDATE_EMAIL.equals(normalizedScene)) && isEmailRegistered) {
            return "EMAIL_USED";
        }
        if (SCENE_RESET_PASSWORD.equals(normalizedScene) && !isEmailRegistered) {
            return "EMAIL_NOT_FOUND";
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. 同邮箱 + 场景重发冷却
        EmailVerificationCode latest = emailVerificationCodeMapper.selectOne(new LambdaQueryWrapper<EmailVerificationCode>()
                .eq(EmailVerificationCode::getEmail, email)
                .eq(EmailVerificationCode::getScene, normalizedScene)
                .orderByDesc(EmailVerificationCode::getCreatedAt)
                .last("LIMIT 1"));
        if (latest != null && latest.getCreatedAt() != null
                && latest.getCreatedAt().plusSeconds(properties.getResendCooldownSeconds()).isAfter(now)) {
            return RATE_LIMIT;
        }

        // 2. 同邮箱当日总量
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        long emailSendsToday = emailVerificationCodeMapper.selectCount(new LambdaQueryWrapper<EmailVerificationCode>()
                .eq(EmailVerificationCode::getEmail, email)
                .ge(EmailVerificationCode::getCreatedAt, dayStart));
        if (emailSendsToday >= properties.getPerEmailDailyLimit()) {
            return RATE_LIMIT;
        }

        // 3. 同 IP 每小时 / 每日总量
        if (requestIp != null && !requestIp.isBlank()) {
            long ipSendsLastHour = emailVerificationCodeMapper.selectCount(new LambdaQueryWrapper<EmailVerificationCode>()
                    .eq(EmailVerificationCode::getRequestIp, requestIp)
                    .ge(EmailVerificationCode::getCreatedAt, now.minusHours(1)));
            if (ipSendsLastHour >= properties.getPerIpHourlyLimit()) {
                return RATE_LIMIT;
            }
            long ipSendsToday = emailVerificationCodeMapper.selectCount(new LambdaQueryWrapper<EmailVerificationCode>()
                    .eq(EmailVerificationCode::getRequestIp, requestIp)
                    .ge(EmailVerificationCode::getCreatedAt, dayStart));
            if (ipSendsToday >= properties.getPerIpDailyLimit()) {
                return RATE_LIMIT;
            }
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        EmailVerificationCode entity = new EmailVerificationCode();
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setEmail(email);
        entity.setScene(normalizedScene);
        entity.setCodeHash(passwordEncoder.encode(code));
        entity.setExpiresAt(now.plusMinutes(properties.getExpireMinutes()));
        entity.setAttemptCount(0);
        entity.setRequestIp(requestIp);
        entity.setCreatedAt(now);
        emailVerificationCodeMapper.insert(entity);

        mailer.send(email, normalizedScene, code);
        return null;
    }

    @Override
    public boolean verifyCode(String email, String code, String scene) {
        return matchCode(email, code, scene, false);
    }

    @Override
    public boolean consumeCode(String email, String code, String scene) {
        return matchCode(email, code, scene, true);
    }

    /**
     * @param shouldConsume true 则校验通过后标记为已用
     */
    private boolean matchCode(String email, String code, String scene, boolean shouldConsume) {
        EmailVerificationCode entity = latestValid(email, scene);
        if (entity == null) {
            return false;
        }
        if (!passwordEncoder.matches(code, entity.getCodeHash())) {
            // 记一次失败；达到上限后 latestValid 不再返回它，等效作废
            emailVerificationCodeMapper.update(null, new LambdaUpdateWrapper<EmailVerificationCode>()
                    .setSql("attempt_count = attempt_count + 1")
                    .eq(EmailVerificationCode::getId, entity.getId()));
            return false;
        }
        if (shouldConsume) {
            // 条件更新 + 行数校验：并发下同一验证码只能被消费一次
            int updated = emailVerificationCodeMapper.update(null, new LambdaUpdateWrapper<EmailVerificationCode>()
                    .set(EmailVerificationCode::getUsedAt, LocalDateTime.now())
                    .eq(EmailVerificationCode::getId, entity.getId())
                    .isNull(EmailVerificationCode::getUsedAt));
            return updated == 1;
        }
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
                .lt(EmailVerificationCode::getAttemptCount, properties.getMaxVerifyAttempts())
                .orderByDesc(EmailVerificationCode::getCreatedAt)
                .last("LIMIT 1"));
    }

    private String normalizeScene(String scene) {
        return scene == null || scene.isBlank() ? SCENE_REGISTER : scene.trim();
    }
}
