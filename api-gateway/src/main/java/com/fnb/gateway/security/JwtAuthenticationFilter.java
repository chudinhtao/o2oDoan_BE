package com.fnb.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * GlobalFilter — chạy trước MỌI request.
 * 1. Kiểm tra nếu route là public → skip
 * 2. Extract Bearer token → check Blacklist (Redis) → validate JWT
 * 3. Forward X-User-Id + X-User-Role headers xuống downstream
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    @Value("${jwt.secret}")
    private String jwtSecret;
	
    @Value("${internal.secret}")
    private String internalSecret;

    private final ReactiveStringRedisTemplate redisTemplate;

    // Danh sách paths không cần JWT
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/menu/**",
            "/api/promotions/**",
            "/api/orders/cart/**",
            "/api/sessions/*",
            "/api/orders/tickets/**",
            "/api/orders/session/**",
            "/api/orders/request-payment",
            "/api/staff-calls/**",
            "/api/payments/**",
            "/api/webhooks/**",
            "/api/customer/ai/**",
            "/ws/**",
            "/actuator/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public int getOrder() {
        return -100; // Chạy sớm nhất
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path       = exchange.getRequest().getURI().getPath();
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // ── BƯỚC 1: Public paths → luôn forward, bất kể token tình trạng nào ──
        // Ví dụ: customer có JWT hết hạn vẫn được xem menu, không bị 401
        if (isPublicPath(path)) {
            log.debug("Public path [{}] — forwarding with internal secret only", path);
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-Internal-Secret", internalSecret)
                    // Xóa Authorization header để downstream không cần xử lý JWT
                    .headers(h -> h.remove(HttpHeaders.AUTHORIZATION))
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        // ── BƯỚC 2: Protected paths → bắt buộc có Bearer token ──
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange.getResponse(), "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        // ── BƯỚC 3: Kiểm tra Blacklist trong Redis ──
        return redisTemplate.hasKey(BLACKLIST_PREFIX + token)
                .flatMap(isBlacklisted -> {
                    if (Boolean.TRUE.equals(isBlacklisted)) {
                        log.warn("Blacklisted token used — path={}", path);
                        return unauthorized(exchange.getResponse(), "Token has been revoked");
                    }

                    // ── BƯỚC 4: Validate JWT → inject user context xuống downstream ──
                    try {
                        Claims claims = validateAndExtract(token);
                        String userId = claims.get("userId", String.class);
                        String role   = claims.get("role", String.class);

                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                .header("X-User-Id",         userId != null ? userId : "")
                                .header("X-User-Role",       role   != null ? role   : "")
                                .header("X-Internal-Secret", internalSecret)
                                .build();

                        log.debug("JWT OK — userId={}, role={}, path={}", userId, role, path);
                        return chain.filter(exchange.mutate().request(mutatedRequest).build());

                    } catch (ExpiredJwtException e) {
                        return unauthorized(exchange.getResponse(), "Token expired");
                    } catch (JwtException e) {
                        return unauthorized(exchange.getResponse(), "Invalid token");
                    }
                });
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    private Claims validateAndExtract(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        log.warn("Unauthorized: {}", message);
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        var body = response.bufferFactory()
                .wrap(("{\"success\":false,\"message\":\"" + message + "\"}").getBytes());
        return response.writeWith(Mono.just(body));
    }
}
