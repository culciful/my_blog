package com.culciful.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @TableName blog_comment
 */
@TableName(value ="blog_comment")
@Data
public class BlogComment implements Serializable {
    @TableId(type = IdType.INPUT)
    private Integer id;

    private Integer commentId;

    private Integer userId;

    private Integer blogId;

    private Integer authorId;

    private Integer isMarkdown;

    private Integer textId;

    private Integer atUserId;

    private Integer parent;

    private Integer root;

    private Date createTime;

    private Date updateTime;

    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}