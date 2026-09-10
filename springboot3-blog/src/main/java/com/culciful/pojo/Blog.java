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
     * 列表展示摘要（对外 JSON key = "abstract"；Java 里 abstract 是关键字所以字段叫 abstractText）。
     * 作者自填时 = 其原文（剥 markdown + 截 200）；未自填时从正文自动生成。
     */
    @TableField("abstract")
    private String abstractText;

    /**
     * abstract 是否作者自填。true：编辑页回填输入框、改正文不重算；false：从正文自动生成
     */
    @TableField("is_custom_abstract")
    private Boolean isCustomAbstract;

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