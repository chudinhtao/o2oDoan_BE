package com.fnb.auth.security;

import com.fnb.auth.security.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.List;

/**
 * Fallback filter: khi không có X-User-Id header (gọi thẳng port 8081, không qua Gateway),
 * tự parse Bearer JWT để set SecurityContext.
 * Chạy SAU GatewayHeaderFilter — nếu đã có auth rồi thì bỏ qua.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtDirectFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Nếu SecurityContext đã có auth (từ GatewayHeaderFilter) → bỏ qua
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
                !(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof String principal
                        && principal.equals("anonymousUser"))) {
            filterChain.doFilter(request, response);
            return;
        }

        // Nếu chưa có auth, parse Bearer token
        String bearer = request.getHeader("Authorization");
        if (!StringUtils.hasText(bearer) || !bearer.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = bearer.substring(7);

        // ★ Kiểm tra Blacklist: token đã bị Logout/Revoke chưa?
        String blacklistKey = "auth:blacklist:" + token;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            log.warn("JwtDirectFilter: token is blacklisted (revoked). IP={}", request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Token has been revoked. Please login again.\"}");
            return;
        }

        try {
            Claims claims = jwtUtil.extractClaims(token);
            String userId = (String) claims.get("userId");
            String role   = (String) claims.get("role");

            if (StringUtils.hasText(userId) && StringUtils.hasText(role)) {
                var auth = new UsernamePasswordAuthenticationToken(
                        userId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("JwtDirectFilter: userId={}, role={}", userId, role);
            }
        } catch (Exception e) {
            log.warn("JwtDirectFilter: invalid token — {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
