package com.fnb.ai.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

/**
 * Feign client gọi sang order-service để gọi nhân viên.
 * Header X-Session-Token được tự động forward bởi FeignHeaderInterceptor.
 */
@FeignClient(name = "order-service",    contextId = "orderClient1")

public interface OrderFeignClient {

    @PostMapping("/api/staff-calls")
    String callStaff(
            @RequestHeader("X-Session-Token") String sessionToken,
            @RequestBody StaffCallBody body
    );

    record StaffCallBody(UUID sessionId, String callType, String message) {}

    @org.springframework.web.bind.annotation.GetMapping("/api/tables/pos")
    com.fnb.common.dto.ApiResponse<java.util.List<PosTableRow>> getAllTablesForPos();

    record PosTableRow(
            UUID id,
            Integer number,
            String name,
            String status,
            Integer capacity,
            UUID currentSessionId,
            String currentSessionToken,
            java.math.BigDecimal totalAmount,
            java.time.LocalDateTime openedAt,
            String zone
    ) {}

    // Deep Order APIs (Phase 5)
    @org.springframework.web.bind.annotation.GetMapping("/api/orders/history")
    com.fnb.common.dto.ApiResponse<Object> getOrderHistory(
            @org.springframework.web.bind.annotation.RequestParam(required = false, value = "status") String status,
            @org.springframework.web.bind.annotation.RequestParam(required = false, value = "startDate") String startDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false, value = "endDate") String endDate,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0", value = "page") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20", value = "size") int size
    );

    @org.springframework.web.bind.annotation.GetMapping("/api/orders/{id}")
    com.fnb.common.dto.ApiResponse<Object> getOrderById(
            @org.springframework.web.bind.annotation.PathVariable("id") UUID id
    );

    @org.springframework.web.bind.annotation.GetMapping("/api/orders/{id}/timeline")
    com.fnb.common.dto.ApiResponse<Object> getOrderTimeline(
            @org.springframework.web.bind.annotation.PathVariable("id") UUID id
    );
}
