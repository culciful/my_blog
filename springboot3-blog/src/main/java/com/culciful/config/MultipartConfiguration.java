package com.culciful.config;

import jakarta.servlet.MultipartConfigElement;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * multipart 第一道闸：请求体超限直接被容器拒掉，不进业务代码。
 * 精细的按用途大小校验在 {@link com.culciful.service.ImageStorageService}。
 */
@Configuration
@RequiredArgsConstructor
public class MultipartConfiguration {

    private final UploadProperties uploadProperties;

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        long maxFile = uploadProperties.getImageMaxSize().toBytes();
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofBytes(maxFile));
        factory.setMaxRequestSize(DataSize.ofBytes(maxFile + DataSize.ofMegabytes(1).toBytes()));
        return factory.createMultipartConfig();
    }
}
