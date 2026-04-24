package com.culciful.utils;

import java.util.Date;
import java.util.Random;

/**
 * @author culciful_zy
 * @version 1.0
 * date 2024/2/2 18:13
 * description:
 */
public class UserIdGenerator {
    public static Long generate() {
        long timestamp = new Date().getTime();
        int random = new Random().nextInt(999);
        return timestamp + random;
    }
}
