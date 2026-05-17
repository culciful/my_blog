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
 * Comment table
 * </p>
 *
 * @author culciful
 * @since 2026-04-28
 */
@Getter
@Setter
@TableName("blog_comment")
public class BlogComment {

    /**
     * Snowflake ID / comment ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * Comment user ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * Article ID
     */
    @TableField("blog_id")
    private Long blogId;

    /**
     * Article author user ID
     */
    @TableField("author_id")
    private Long authorId;

    /**
     * Markdown flag
     */
    @TableField("is_markdown")
    private Boolean isMarkdown;

    /**
     * Comment content text_body.id
     */
    @TableField("content_text_id")
    private Long contentTextId;

    /**
     * Mentioned user ID
     */
    @TableField("at_user_id")
    private Long atUserId;

    /**
     * Parent comment ID
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * Root comment ID
     */
    @TableField("root_id")
    private Long rootId;

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