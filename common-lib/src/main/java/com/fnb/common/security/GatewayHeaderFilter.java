package com.fnb.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Dùng ở các DOWNSTREAM services (không dùng ở api-gateway).
 * Đọc X-User-Id và X-User-Role headers do Gateway forward,
 * rồi set vào SecurityContext để @PreAuthorize hoạt động.
 */
@Slf4j
@Component
public class GatewayHeaderFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID   = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader(HEADER_USER_ID);
        String role   = request.getHeader(HEADER_USER_ROLE);

        if (StringUtils.hasText(userId) && StringUtils.hasText(role)) {
            var authority = new SimpleGrantedAuthority("ROLE_" + role.toUpperCase());
            var auth = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of(authority));
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("GatewayHeaderFilter: userId={}, role={}", userId, role);
        }

        filterChain.doFilter(request, response);
    }
}
