package com.culciful.config;

import com.culciful.security.JwtAuthenticationFilter;
import com.culciful.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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
