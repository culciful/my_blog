package com.culciful.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Article tag relation table
 * </p>
 *
 * @author culciful
 * @since 2026-04-28
 */
@Getter
@Setter
@TableName("blog_tag_relation")
public class BlogTagRelation {
    /**
     * Snowflake ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * Article ID
     */
    @TableField("blog_id")
    private Long blogId;

    /**
     * Tag ID
     */
    @TableField("tag_id")
    private Long tagId;
}