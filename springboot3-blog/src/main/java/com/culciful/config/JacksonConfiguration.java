package com.culciful.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * 雪花 ID（19 位 long）超出 JavaScript Number 的安全整数范围（2^53-1），
 * 直接以 JSON 数字返回会在前端精度丢失。这里把「超出安全范围的 Long」序列化为字符串，
 * 安全范围内的 Long（时间戳秒、计数等）保持数字，尽量不影响其它字段。
 */
@Configuration
public class JacksonConfiguration {

    private static final long MAX_SAFE_INTEGER = 9007199254740991L;

    static class SafeLongSerializer extends JsonSerializer<Long> {
        @Override
        public void serialize(Long value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else if (value > MAX_SAFE_INTEGER || value < -MAX_SAFE_INTEGER) {
                gen.writeString(value.toString());
            } else {
                gen.writeNumber(value);
            }
        }
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer safeLongSerialization() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, new SafeLongSerializer());
            module.addSerializer(Long.TYPE, new SafeLongSerializer());
            builder.modulesToInstall(module);
        };
    }
}
