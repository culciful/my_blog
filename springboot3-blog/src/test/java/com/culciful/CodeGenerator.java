package com.culciful;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.TemplateType;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.nio.file.Paths;
import java.util.Collections;

/**
 * MyBatis-Plus code generator.
 *
 * <p>Default strategy: generate entity + mapper only, keep service/controller handwritten.</p>
 */
public class CodeGenerator {

    private static String getenv(String key, String fallback) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? fallback : v;
    }

    public static void main(String[] args) {
        // Local codegen tool: set DB_URL / DB_USERNAME / DB_PASSWORD env vars, or edit the fallbacks.
        String url = getenv("DB_URL", "jdbc:mysql://127.0.0.1:3306/culciful_blog?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC");
        String username = getenv("DB_USERNAME", "root");
        String password = getenv("DB_PASSWORD", "root");

        String projectPath = System.getProperty("user.dir");
        String outputDir = Paths.get(projectPath, "target", "generated-sources", "mp", "java").toString();
        String mapperXmlDir = Paths.get(projectPath, "target", "generated-sources", "mp", "mapper").toString();

        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> builder
                        .author("culciful")
                        .disableOpenDir()
                        .outputDir(outputDir)
                        .dateType(DateType.TIME_PACK)
                        .commentDate("yyyy-MM-dd")
                )
                .packageConfig(builder -> builder
                        .parent("com.culciful")
                        .entity("pojo")
                        .mapper("mapper")
                        .pathInfo(Collections.singletonMap(OutputFile.xml, mapperXmlDir))
                )
                .templateConfig(builder -> builder
                        .disable(TemplateType.SERVICE, TemplateType.SERVICE_IMPL, TemplateType.CONTROLLER)
                )
                .strategyConfig(builder -> builder
                        .addInclude(
                                "user_info",
                                "user_package",
                                "user_follow",
                                "blog",
                                "blog_comment",
                                "blog_tag",
                                "blog_tag_relation",
                                "text_body"
                        )
                        .entityBuilder()
                        .enableLombok()
                        .enableTableFieldAnnotation()
                        .naming(NamingStrategy.underline_to_camel)
                        .columnNaming(NamingStrategy.underline_to_camel)
                        .idType(IdType.INPUT)
                        .disableSerialVersionUID()
                        .mapperBuilder()
                        .enableMapperAnnotation()
                        .enableBaseResultMap()
                        .enableBaseColumnList()
                )
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }
}
