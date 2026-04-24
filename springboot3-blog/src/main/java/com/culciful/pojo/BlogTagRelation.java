package com.culciful.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * @TableName blog_tag_relation
 */
@TableName(value ="blog_tag_relation")
@Data
public class BlogTagRelation implements Serializable {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer blogId;

    private String tag;

    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}