package com.culciful.service;

public interface EmailVerificationCodeService {
    String SCENE_REGISTER = "register";
    String SCENE_RESET_PASSWORD = "reset";
    String SCENE_UPDATE_EMAIL = "update_email";

    /**
     * @param requestIp 发起请求的客户端 IP，用于按 IP 限流；可为 null
     * @return null on success, otherwise a business error reason:
     *         EMAIL_USED / EMAIL_NOT_FOUND / RATE_LIMIT
     */
    String sendCode(String email, String scene, String requestIp);

    boolean verifyCode(String email, String code, String scene);

    boolean consumeCode(String email, String code, String scene);
}
