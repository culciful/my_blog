package com.culciful.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Security audit log table
 * </p>
 */
@Getter
@Setter
@TableName("audit_log")
public class AuditLog {

    /**
     * Snowflake ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * Acting user ID; null when the user couldn't be identified (e.g. a failed login)
     */
    @TableField("user_id")
    private Long userId;

    /**
     * Event type, see {@link com.culciful.common.enums.AuditAction}
     */
    @TableField("action")
    private String action;

    /**
     * Source IP
     */
    @TableField("ip")
    private String ip;

    /**
     * Extra context, e.g. the submitted username/email on a failed login
     */
    @TableField("detail")
    private String detail;

    /**
     * Created time (UTC)
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
