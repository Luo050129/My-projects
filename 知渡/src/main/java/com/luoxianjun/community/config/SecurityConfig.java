package com.luoxianjun.community.config;

import com.luoxianjun.community.security.JwtAuthFilter;
import com.luoxianjun.community.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 安全配置：无状态会话 + JWT 过滤器 + 方法级鉴权(@PreAuthorize)。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 注意：server.servlet.context-path=/api 已由容器剥离，
                // 此处 matcher 须用「去掉 context-path 之后」的路径
                // 公开接口：认证、健康检查、H2 控制台
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/actuator/health", "/h2-console/**").permitAll()
                // 内容读接口公开
                // /posts/* 只匹配单段，三段路径必须显式列出，否则匿名读交互态会被 401
                .requestMatchers(HttpMethod.GET, "/posts", "/posts/*", "/posts/*/interaction", "/comments/posts/**").permitAll()
                // 用户公开主页与社交关系可匿名读（/user/page 另有 @PreAuthorize 兜住）
                .requestMatchers(HttpMethod.GET, "/user/*", "/user/*/followers", "/user/*/followees")
                    .permitAll()
                // 上传的图片需公开可读（img 标签无法带 Authorization 头，必须放行 GET）
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                // 种子帖封面（随 jar 打包的静态资源）
                .requestMatchers(HttpMethod.GET, "/seed/**").permitAll()
                // 其余均需要登录
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("*"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
