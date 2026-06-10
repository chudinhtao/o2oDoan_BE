package com.fnb.inventory.config;

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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Admin endpoints: require authentication (enforce with @PreAuthorize)
                        .requestMatchers("/api/admin/inventory/**").authenticated()
                        // Actuator
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(internalSecretFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(gatewayHeaderFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
