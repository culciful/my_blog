package com.culciful;

import com.culciful.security.token.JwtHelper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@org.springframework.boot.test.context.SpringBootTest
public class TestUtils {

    @Autowired
    private JwtHelper jwtHelper;

    @Test
    public void testJwt() {
        // 生成 传入用户标识 + token 版本号
        String token = jwtHelper.createToken(1L, 0L);
        System.out.println("token = " + token);

        // 解析用户标识
        int userId = jwtHelper.getUserId(token).intValue();
        System.out.println("userId = " + userId);

        // 校验是否到期! false 未到期 true到期
        boolean isExpired = jwtHelper.isExpired(token);
        System.out.println("isExpired = " + isExpired);
    }
}

