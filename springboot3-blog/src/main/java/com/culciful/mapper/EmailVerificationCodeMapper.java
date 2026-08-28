package com.culciful.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.culciful.pojo.EmailVerificationCode;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmailVerificationCodeMapper extends BaseMapper<EmailVerificationCode> {
}
