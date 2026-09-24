package com.culciful.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Tag table
 * </p>
 *
 * @author culciful
 * @since 2026-04-28
 */
@Getter
@Setter
@TableName("blog_tag")
public class BlogTag {
    /**
     * Snowflake ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * Tag name
     */
    @TableField("tag")
    private String tag;
}