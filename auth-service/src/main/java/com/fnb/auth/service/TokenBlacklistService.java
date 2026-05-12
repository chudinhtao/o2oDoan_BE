package com.fnb.auth.service;

import com.fnb.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Date;

/**
 * Quản lý danh sách đen Access Token (JWT) trong Redis.
 * TTL của entry = thời gian sống còn lại của token, đảm bảo Redis tự dọn dẹp.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    /**
     * Thêm Access Token vào Blacklist với TTL = thời gian sống còn lại.
     */
    public void blacklist(String token) {
        if (!StringUtils.hasText(token)) return;

        try {
            Date expiration = jwtUtil.extractExpiration(token);
            long ttlMillis  = expiration.getTime() - System.currentTimeMillis();

            if (ttlMillis > 0) {
                redisTemplate.opsForValue().set(
                        BLACKLIST_PREFIX + token,
                        "1",
                        Duration.ofMillis(ttlMillis)
                );
                log.debug("Token blacklisted, TTL={}ms", ttlMillis);
            }
        } catch (Exception e) {
            // Token đã hết hạn hoặc không hợp lệ — không cần blacklist
            log.warn("Blacklist skip: {}", e.getMessage());
        }
    }

    /**
     * Kiểm tra token có nằm trong Blacklist không.
     */
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(BLACKLIST_PREFIX + token)
        );
    }
}
