package com.fnb.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO đại diện cho một nhóm món cần bưng ra cùng một bàn.
 * Group by tableNumber để Server biết cần bưng tất cả món một lần.
 */
@Data
@Builder
public class TicketDeliveryDto {

    private Integer tableNumber;
    private UUID tableId;
    private String zone;
    private List<DeliveryItem> items;

    @Data
    @Builder
    public static class DeliveryItem {
        private UUID itemId;
        private String itemName;
        private Integer quantity;
        private String station;      // HOT, COLD, DRINK
        private String status;       // DONE, COMPLETED
        private LocalDateTime readyAt; // Thời điểm bếp hoàn thành (created_at khi DONE)
        private BigDecimal unitPrice;
        private String note;
        private boolean isUrgent;    // true nếu chờ > 60s (HOT) hoặc > 0s (COLD)
    }
}
