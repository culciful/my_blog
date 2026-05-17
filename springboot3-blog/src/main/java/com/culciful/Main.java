package com.culciful;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author culciful_zy
 * @version 1.0
 * date 2024/1/29 16:29
 * description:
 */
@SpringBootApplication
@MapperScan("com.culciful.mapper")
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    // Configure MyBatis-Plus plugins.
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL)); // Pagination
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());  // Optimistic locking
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());  // Block full-table update/delete
        return interceptor;
    }
}
