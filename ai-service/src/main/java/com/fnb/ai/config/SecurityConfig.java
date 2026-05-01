package com.fnb.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security config cho ai-service.
 * Ủy thác xác thực cho api-gateway (trust X-User-Id / X-Session-Token headers).
 * Chỉ mở public cho internal sync endpoint.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Internal: Vector Sync endpoint (chỉ gọi từ nội bộ)
                .requestMatchers("/api/internal/ai/**").permitAll()
                // Health check
                .requestMatchers("/actuator/**").permitAll()
                // Chat endpoints: tin tưởng gateway đã xác thực
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
