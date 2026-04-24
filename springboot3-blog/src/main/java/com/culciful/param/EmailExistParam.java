package com.culciful.param;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author culciful_zy
 * @version 1.0
 * date 2024/4/20 18:49
 * description:使用jsr 303注解进行校验
 */
@Data
public class EmailExistParam {
    @NotBlank // 不为null和空字符串
    private String email;
}
