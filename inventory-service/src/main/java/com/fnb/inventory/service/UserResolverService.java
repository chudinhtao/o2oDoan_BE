package com.fnb.inventory.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolve UUID → full_name từ auth.users (cross-schema native query).
 * Cache in-memory để tránh query lặp lại.
 */
@Service
@RequiredArgsConstructor
public class UserResolverService {

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String resolveName(String userId) {
        if (userId == null || userId.isBlank()) return null;

        // Nếu không phải UUID hợp lệ, trả về nguyên bản
        try { UUID.fromString(userId); }
        catch (IllegalArgumentException e) { return userId; }

        return cache.computeIfAbsent(userId, id -> {
            try {
                String name = jdbcTemplate.queryForObject(
                    "SELECT full_name FROM auth.users WHERE id = ?::uuid",
                    String.class, id
                );
                return name != null ? name : id;
            } catch (Exception e) {
                return id; // Fallback: trả về UUID nếu không tìm thấy
            }
        });
    }
}
