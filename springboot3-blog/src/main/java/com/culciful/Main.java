package com.culciful;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @author culciful_zy
 * @version 1.0
 * date 2024/1/29 16:29
 * description:
 */
// freemarker 只是 test 域下 mybatis-plus-generator 生成代码用的模板引擎，这个项目不用它渲染
// 网页视图（纯 JSON REST）；测试类路径上一有 freemarker.jar，Boot 就会尝试自动配一个
// FreeMarkerConfigurer 视图解析器 bean，而这个 bean 的方法签名会反射到需要 freemarker-servlet
// 才有的 TaglibFactory，我们没引那个子模块，直接 NoClassDefFoundError 炸掉整个测试上下文
// （Spring Boot 3.5.x 起对这个 bean 的自省更严格，3.2.0 时没炸）——排除掉，反正用不上
@SpringBootApplication(exclude = FreeMarkerAutoConfiguration.class)
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
