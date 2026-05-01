package com.fnb.order.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event bắn bởi Sweeper Job khi phát hiện món đã xong (DONE) nhưng
 * chưa được bưng trong ngưỡng thời gian quy định.
 * Dùng cho: Dashboard Server hiển thị cảnh báo đỏ, Haptic + Audio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryReadyAlertEvent {
    private Integer tableNumber;
    private UUID tableId;
    private String zone;
    private List<UUID> urgentItemIds;
    private String urgencyLevel; // "WARNING" (>30s), "CRITICAL" (>60s)
    private LocalDateTime alertAt;
}
