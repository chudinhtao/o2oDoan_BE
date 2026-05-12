package com.fnb.ai.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fnb.common.filter.InternalSecretFilter;
import com.fnb.common.security.GatewayHeaderFilter;



/**
 * Security config cho ai-service.
 * Ủy thác xác thực cho api-gateway (trust X-User-Id / X-Session-Token headers).
 * Chỉ mở public cho internal sync endpoint.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public InternalSecretFilter internalSecretFilter() {
        return new InternalSecretFilter();
    }

    @Bean
    public GatewayHeaderFilter gatewayHeaderFilter() {
        return new GatewayHeaderFilter();
    }

    @Bean
    public FilterRegistrationBean<InternalSecretFilter> internalSecretFilterRegistration(InternalSecretFilter filter) {
        FilterRegistrationBean<InternalSecretFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<GatewayHeaderFilter> gatewayHeaderFilterRegistration(GatewayHeaderFilter filter) {
        FilterRegistrationBean<GatewayHeaderFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

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
            )
            .addFilterBefore(internalSecretFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(gatewayHeaderFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
