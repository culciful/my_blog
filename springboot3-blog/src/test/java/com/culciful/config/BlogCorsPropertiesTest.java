package com.culciful.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BlogCorsPropertiesTest {

    @Test
    void resolvedAllowedOriginsDropsBlankEntries() {
        BlogCorsProperties props = new BlogCorsProperties();
        // 生产 ${BLOG_PUBLIC_ORIGIN:} 未注入时可能绑成 [""]
        props.setAllowedOrigins(List.of("", "  ", "https://app.example.com "));

        assertThat(props.resolvedAllowedOrigins())
                .containsExactly("https://app.example.com");
    }

    @Test
    void emptyOriginsResolveToEmptyList() {
        assertThat(new BlogCorsProperties().resolvedAllowedOrigins()).isEmpty();
    }

    @Test
    void defaultsAreConvergedToWhatTheFrontendActuallyUses() {
        BlogCorsProperties props = new BlogCorsProperties();
        assertThat(props.getAllowedHeaders()).containsExactly("Content-Type");
        assertThat(props.getAllowedMethods()).containsExactly("GET", "POST", "OPTIONS");
    }
}
