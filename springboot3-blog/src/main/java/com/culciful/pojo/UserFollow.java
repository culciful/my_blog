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
 * User follow relation table
 * </p>
 *
 * @author culciful
 * @since 2026-04-28
 */
@Getter
@Setter
@TableName("user_follow")
public class UserFollow {
    /**
     * Snowflake ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * Followed user ID
     */
    @TableField("following_id")
    private Long followingId;

    /**
     * Follower user ID
     */
    @TableField("follower_id")
    private Long followerId;

    /**
     * Created time (UTC)
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * Updated time (UTC)
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}