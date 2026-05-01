package com.fnb.common.security;

import com.fnb.common.exception.UnauthorizedException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    // ─── Key ─────────────────────────────────────────────────────────────

    private SecretKey getKey() {
        // Dùng ít nhất 32 ký tự để đủ 256-bit
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ─── Generate ─────────────────────────────────────────────────────────

    /**
     * Tạo access token
     * @param claims  extra claims: userId, role, ...
     * @param subject username
     */
    public String generateToken(Map<String, Object> claims, String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtProperties.getExpiry() * 1000))
                .signWith(getKey())
                .compact();
    }

    /**
     * Tạo refresh token (chỉ chứa subject)
     */
    public String generateRefreshToken(String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtProperties.getRefreshExpiry() * 1000))
                .signWith(getKey())
                .compact();
    }

    // ─── Validate ─────────────────────────────────────────────────────────

    /**
     * @return true nếu token hợp lệ, false nếu expired hoặc signature sai
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.warn("JWT invalid: {}", e.getMessage());
            return false;
        }
    }

    // ─── Extract ──────────────────────────────────────────────────────────

    public Claims extractClaims(String token) {
        try {
            return parseClaims(token);
        } catch (JwtException e) {
            throw new UnauthorizedException("Invalid token");
        }
    }

    public String extractUserId(String token) {
        return (String) extractClaims(token).get("userId");
    }

    public String extractRole(String token) {
        return (String) extractClaims(token).get("role");
    }

    public String extractSubject(String token) {
        return extractClaims(token).getSubject();
    }

    // ─── Private ──────────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
