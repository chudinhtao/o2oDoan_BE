package com.fnb.ai.feign;

import com.fnb.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@FeignClient(name = "order-service",   contextId = "orderClient2")
public interface ReservationFeignClient {

    @GetMapping("/api/customer/reservations/check-capacity")
    ApiResponse<Boolean> checkCapacity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime time,
            @RequestParam int partySize);

    @GetMapping("/api/admin/reservations")
    ApiResponse<PageResponse<ReservationRow>> getAdminReservations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size);

    // ─── Response Records ─────────────────────────────────────────────────────

    record PageResponse<T>(
            List<T> content,
            int pageNumber,
            int pageSize,
            long totalElements,
            int totalPages,
            boolean last
    ) {}

    record ReservationRow(
            UUID id,
            String customerName,
            String customerPhone,
            Integer partySize,
            Integer adultCount,
            Integer childrenCount,
            LocalDateTime bookingTime,
            String status,
            BigDecimal depositAmount,
            String refundStatus,
            String preOrderDraft,
            String note,
            List<Integer> assignedTableNumbers
    ) {}
}
