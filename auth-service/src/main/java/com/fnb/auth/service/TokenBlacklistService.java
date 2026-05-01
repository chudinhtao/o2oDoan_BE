package com.fnb.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    /**
     * Cho token vào danh sách bị cấm.
     * @param token JWT cần vô hiệu hóa
     * @param duration Thời gian sống còn lại của token (sẽ tự xóa khỏi Redis khi hết hạn JWT)
     */
    public void blacklistToken(String token, long durationInSeconds) {
        if (durationInSeconds > 0) {
            redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + token, 
                "true", 
                Duration.ofSeconds(durationInSeconds)
            );
        }
    }

    /**
     * Kiểm tra token có bị cấm không.
     */
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
