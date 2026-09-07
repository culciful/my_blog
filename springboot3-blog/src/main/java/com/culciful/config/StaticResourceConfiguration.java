package com.culciful.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class StaticResourceConfiguration implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploads = Path.of(uploadProperties.getDir()).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/" + uploadProperties.getDir() + "/**").addResourceLocations(uploads);
    }
}
