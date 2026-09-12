package com.culciful.config;

import com.culciful.security.JwtAuthenticationFilter;
import com.culciful.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final BlogCorsProperties blogCorsProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(restAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/user/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/user/checkEmailExist").permitAll()
                        .requestMatchers(HttpMethod.POST, "/user/sendEmailCode").permitAll()
                        .requestMatchers(HttpMethod.POST, "/user/checkEmailCode").permitAll()
                        .requestMatchers(HttpMethod.POST, "/user/resetPassword").permitAll()
                        .requestMatchers(HttpMethod.GET, "/user/getUserInfo").permitAll()
                        .requestMatchers(HttpMethod.GET, "/article/getTags").permitAll()
                        .requestMatchers(HttpMethod.GET, "/article/getArticleInfo").permitAll()
                        .requestMatchers(HttpMethod.POST, "/article/getArticleList").permitAll()
                        .requestMatchers(HttpMethod.POST, "/comment/getComments").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 以下三个头 Spring Security 本就有相同的默认值（实测验证过），这里显式写出来，
                // 不靠隐式默认——升级 Spring Security 版本时默认值变了也不会被悄悄影响。
                // CSP 不在这里加：后端只出 JSON，SPA 的 HTML 不经过它，CSP 要加在前端静态托管
                // 层（nginx），且只在生产构建启用（严格 CSP 会断 Vite dev 的 HMR）。
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                // 只在请求本身是 HTTPS 时才发这个头（HTTP 明文响应带 HSTS 没有
                                // 意义，还可能被 MITM 篡改/剥离）；生产反代需配
                                // server.forward-headers-strategy=framework（已配），
                                // 让 request.isSecure() 正确识别反代后面的 HTTPS
                                .maxAgeInSeconds(Duration.ofDays(365).toSeconds())
                                .includeSubDomains(true)
                                // preload 不开：一旦提交进浏览器内置 HSTS 预加载列表几乎不可逆
                                // （要等很久才能从列表移除），对个人博客这种规模没必要冒这个险
                                .preload(false)
                        )
                );

        if (!blogCorsProperties.resolvedAllowedOrigins().isEmpty()) {
            http.cors(c -> c.configurationSource(corsConfigurationSource()));
        }

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(blogCorsProperties.resolvedAllowedOrigins());
        config.setAllowedMethods(blogCorsProperties.getAllowedMethods());
        config.setAllowedHeaders(blogCorsProperties.getAllowedHeaders());
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
