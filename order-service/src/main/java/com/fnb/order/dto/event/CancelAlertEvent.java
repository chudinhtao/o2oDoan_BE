package com.fnb.order.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event bắn khi có món hủy ở trạng thái DONE (đã làm xong, đang chờ bưng).
 * Mục đích: Cảnh báo đỏ + Haptic trên app Server để tránh bưng nhầm.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelAlertEvent {
    private UUID orderId;
    private Integer tableNumber;
    private List<CancelledItem> cancelledItems;
    private LocalDateTime cancelledAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelledItem {
        private UUID itemId;
        private String itemName;
        private Integer quantity;
        private String station;
    }
}
