package com.fnb.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter dùng chung cho TẤT CẢ downstream services.
 * Đảm bảo 100% request đi vào phải có X-Internal-Secret hợp lệ.
 * Trừ các endpoint public nội bộ như /actuator.
 */
@Slf4j
public class InternalSecretFilter extends OncePerRequestFilter {

    @Value("${internal.secret}")
    private String internalSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Bỏ qua cho actuator health check
        if (request.getRequestURI().startsWith("/actuator/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String secret = request.getHeader("X-Internal-Secret");

        if (!StringUtils.hasText(secret) || !secret.equals(internalSecret)) {
            log.warn("[SECURITY ALERT] Secret mismatch! Received: {}, Expected (partial): {}...", 
                    secret != null ? "PRESENT" : "MISSING",
                    (internalSecret != null && internalSecret.length() > 5) ? internalSecret.substring(0, 5) : "NULL/SHORT");
            
            log.warn("[SECURITY ALERT] Direct access attempt without Internal Secret! IP: {}, Path: {}",
                    request.getRemoteAddr(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Internal Access Denied\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
