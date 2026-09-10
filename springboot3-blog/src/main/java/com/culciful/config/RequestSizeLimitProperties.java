package com.culciful.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/**
 * 非 multipart 请求体大小上限。multipart（图片上传）走 {@link MultipartConfiguration}。
 */
@Data
@Component
@ConfigurationProperties(prefix = "blog.request-limit")
public class RequestSizeLimitProperties {

    private DataSize maxBodySize = DataSize.ofMegabytes(1);
}
