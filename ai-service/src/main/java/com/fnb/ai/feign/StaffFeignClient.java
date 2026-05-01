package com.fnb.ai.feign;

import com.fnb.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Feign client goi sang auth-service de lay danh sach nhan vien.
 * Chi dung cho Admin AI Agent — READ-ONLY, khong co side effect.
 *
 * Luu y: /api/admin/staff yeu cau ADMIN role — FeignHeaderInterceptor
 * se inject JWT tu SecurityContext neu co, hoac dung Internal header.
 */
@FeignClient(name = "auth-service")
public interface StaffFeignClient {

    @GetMapping("/api/admin/staff")
    ApiResponse<List<StaffRow>> getAllStaff();

    // ─── Response Record ─────────────────────────────────────────────────────

    record StaffRow(
            UUID id,
            String username,
            String role,        // ADMIN | CASHIER | KITCHEN
            String fullName,
            boolean isActive,
            LocalDateTime createdAt
    ) {}
}
