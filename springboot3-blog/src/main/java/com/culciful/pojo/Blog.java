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
 * Article table
 * </p>
 *
 * @author culciful
 * @since 2026-04-28
 */
@Getter
@Setter
@TableName("blog")
public class Blog {

    /**
     * Snowflake ID / article ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * Author user ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * Title
     */
    @TableField("title")
    private String title;

    /**
     * Summary
     */
    @TableField("overview")
    private String overview;

    /**
     * Content text_body.id
     */
    @TableField("content_text_id")
    private Long contentTextId;

    /**
     * View count
     */
    @TableField("view_count")
    private Integer viewCount;

    /**
     * Comment count
     */
    @TableField("comment_count")
    private Integer commentCount;

    /**
     * Package ID
     */
    @TableField("package_id")
    private Long packageId;

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
}