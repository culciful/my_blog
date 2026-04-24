package com.culciful.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @TableName user_follow
 */
@TableName(value ="user_follow")
@Data
public class UserFollow implements Serializable {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer following;

    private Integer follower;

    private Integer isDeleted;

    private Date updateTime;

    private static final long serialVersionUID = 1L;
}