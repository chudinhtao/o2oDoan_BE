package com.fnb.order.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event bắn bởi Sweeper Job khi StaffCall PENDING quá 30 giây.
 * Mục đích: Cứu viện - broadcast để tất cả Server ở mọi zone
 * đều thấy và có thể hỗ trợ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCallSpilloverEvent {
    private UUID callId;
    private Integer tableNumber;
    private UUID tableId;
    private String zone;
    private String callType;
    private Long pendingSeconds; // Đã chờ bao nhiêu giây
    private LocalDateTime alertAt;
}
