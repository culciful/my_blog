package com.culciful.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @TableName user_package
 */
@TableName(value ="user_package")
@Data
public class UserPackage implements Serializable {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer packId;

    private Integer userId;

    private String packName;

    private Date updateTime;

    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}