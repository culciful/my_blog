package com.culciful.service;

import com.culciful.config.EmailCodeProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 发验证码邮件。配置了 spring.mail.host 就真发（dev 可指向 Mailhog:1025），
 * 否则降级为打日志——本地无邮件服务器时照样能拿到验证码联调。
 */
@Component
@RequiredArgsConstructor
public class VerificationCodeMailer {

    private static final Logger log = LoggerFactory.getLogger(VerificationCodeMailer.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final EmailCodeProperties properties;

    public void send(String email, String scene, String code) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("dev email verification code for {} [{}]: {}", email, scene, code);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.getFrom());
            message.setTo(email);
            message.setSubject("验证码");
            message.setText("你的验证码是 " + code + "，" + properties.getExpireMinutes()
                    + " 分钟内有效。如非本人操作请忽略。");
            sender.send(message);
        } catch (Exception e) {
            // 发信失败不阻断主流程；验证码已入库，用户可重试
            log.warn("failed to send verification email to {} [{}]: {}", email, scene, e.toString());
        }
    }
}
