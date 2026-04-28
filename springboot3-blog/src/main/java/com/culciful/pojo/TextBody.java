package com.culciful.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * @TableName text_body
 */
@TableName(value ="text_body")
@Data
public class TextBody implements Serializable {
    @TableId(type = IdType.INPUT)
    private Integer id;

    private Integer textId;

    private String body;

    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}