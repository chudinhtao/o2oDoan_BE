package com.fnb.menu.config;

import com.fnb.common.filter.InternalSecretFilter;
import com.fnb.common.security.GatewayHeaderFilter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
    public InternalSecretFilter internalSecretFilter() {
        return new InternalSecretFilter();
    }

    @Bean
    public GatewayHeaderFilter gatewayHeaderFilter() {
        return new GatewayHeaderFilter();
    }

    /** Chặn Spring Boot tự register InternalSecretFilter vào servlet chain (tránh chạy 2 lần) */
    @Bean
    public FilterRegistrationBean<InternalSecretFilter> internalSecretFilterRegistration(InternalSecretFilter filter) {
        FilterRegistrationBean<InternalSecretFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    /** Chặn Spring Boot tự register GatewayHeaderFilter vào servlet chain */
    @Bean
    public FilterRegistrationBean<GatewayHeaderFilter> gatewayHeaderFilterRegistration(GatewayHeaderFilter filter) {
        FilterRegistrationBean<GatewayHeaderFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: customer đọc menu, validate promo
                        .requestMatchers("/api/menu/**", "/api/promotions/**").permitAll()
                        // Admin: cần role ADMIN (enforce thêm ở @PreAuthorize)
                        .requestMatchers("/api/admin/**").authenticated()
                        // Actuator
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(internalSecretFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(gatewayHeaderFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
