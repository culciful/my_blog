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
 * User profile table
 * </p>
 *
 * @author culciful
 * @since 2026-04-28
 */
@Getter
@Setter
@TableName("user_info")
public class UserInfo {

    /**
     * Snowflake ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * User email
     */
    @TableField("email")
    private String email;

    /**
     * Username
     */
    @TableField("username")
    private String username;

    /**
     * User password hash
     */
    @TableField("password")
    private String password;

    /**
     * Avatar asset ID
     */
    @TableField("avatar_asset_id")
    private Long avatarAssetId;

    /**
     * Cached article count
     */
    @TableField("article_count")
    private Integer articleCount;

    /**
     * Cached following count
     */
    @TableField("following_count")
    private Integer followingCount;

    /**
     * Cached follower count
     */
    @TableField("follower_count")
    private Integer followerCount;

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

    /**
     * Deleted flag
     */
    @TableField("is_deleted")
    private Boolean isDeleted;

    /**
     * Delete token: active = 0, deleted = unique value
     */
    @TableField("deleted_token")
    private Long deletedToken;
}