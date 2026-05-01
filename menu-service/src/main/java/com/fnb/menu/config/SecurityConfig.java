package com.fnb.menu.config;

import com.fnb.common.security.GatewayHeaderFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public GatewayHeaderFilter gatewayHeaderFilter() {
        return new GatewayHeaderFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: customer đọc menu, validate promo
                        .requestMatchers(
                                "/api/menu/**",
                                "/api/promotions/**"
                        ).permitAll()
                        // Admin: cần role ADMIN (enforce thêm ở @PreAuthorize)
                        .requestMatchers("/api/admin/**").authenticated()
                        // Actuator
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(gatewayHeaderFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

