package com.culciful.service;

public interface EmailVerificationCodeService {
    String SCENE_REGISTER = "register";
    String SCENE_RESET_PASSWORD = "reset";
    String SCENE_UPDATE_EMAIL = "update_email";

    /**
     * @return null on success, otherwise a business error message key/code reason
     */
    String sendCode(String email, String scene);

    boolean verifyCode(String email, String code, String scene);

    boolean consumeCode(String email, String code, String scene);
}
