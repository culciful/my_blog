package com.culciful;

import com.culciful.utils.JwtHelper;
import com.culciful.utils.RSAUtil;
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
        // 生成 传入用户标识
        String token = jwtHelper.createToken(1L);
        System.out.println("token = " + token);

        // 解析用户标识
        int userId = jwtHelper.getUserId(token).intValue();
        System.out.println("userId = " + userId);

        // 校验是否到期! false 未到期 true到期
        boolean expiration = jwtHelper.isExpiration(token);
        System.out.println("expiration = " + expiration);
    }

    // @Test
    // public void testRSA() throws UnsupportedEncodingException {
    //     String publicKey = RSAUtil.getPublicKey();
    //     System.out.println("publicKey = " + publicKey);
    //     String getPrivateKey = RSAUtil.getPrivateKey();
    //
    //     String str = "{username: '123345张三hh', password: '123345'}";
    //     byte[] encrypt = RSAUtil.encrypt(str);
    //     System.out.println("encrypt = " + encrypt);
    //     String decrypt = RSAUtil.decrypt(encrypt);
    //     System.out.println("decrypt = " + decrypt);
    //
    // }
}

