package com.culciful.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/**
 * 图片上传配置。所有值都有默认，`blog.upload.*` 可覆盖。
 */
@Data
@Component
@ConfigurationProperties(prefix = "blog.upload")
public class UploadProperties {

    /** 落盘根目录（相对进程工作目录），同时也是 /uploads/** 静态资源映射的目录 */
    private String dir = "uploads";

    /** 头像单文件上限 */
    private DataSize avatarMaxSize = DataSize.ofMegabytes(2);

    /** 文章 / 评论插图单文件上限 */
    private DataSize imageMaxSize = DataSize.ofMegabytes(5);

    /** 解码后像素总数上限，挡「解压炸弹」（默认 6000 万 ≈ 8000x7500） */
    private long maxPixels = 60_000_000L;
}
