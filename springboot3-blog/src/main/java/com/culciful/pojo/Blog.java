package com.culciful.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @TableName blog
 */
@TableName(value ="blog")
@Data
public class Blog implements Serializable {

    @TableId(type = IdType.INPUT)
    private Integer id;

    private Integer blogId;

    private Integer userId;

    private String title;

    private String overview;

    private Integer textId;

    private Integer viewCount;

    private Integer commentCount;

    private Integer packId;

    private Date createTime;

    private Date updateTime;

    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}