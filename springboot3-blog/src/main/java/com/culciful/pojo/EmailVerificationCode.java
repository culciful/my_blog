package com.culciful.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("email_verification_code")
public class EmailVerificationCode {
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("email")
    private String email;

    @TableField("scene")
    private String scene;

    @TableField("code_hash")
    private String codeHash;

    @TableField("expires_at")
    private LocalDateTime expiresAt;

    @TableField("used_at")
    private LocalDateTime usedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
