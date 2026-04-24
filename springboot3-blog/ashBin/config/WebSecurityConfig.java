package com.culciful.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * @author culciful_zy
 * @version 1.0
 * date 2024/1/31 23:02
 * description:
 */
// @Configuration
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests(
                authorize -> authorize
                        // 对所有请求开启授权保护
                        .anyRequest()
                        // 已认证的请求会被自动授权
                        .authenticated()
        )
                .formLogin(form -> {
                    form.loginPage("/login").permitAll();
                });

        return http.build();
    }
}
