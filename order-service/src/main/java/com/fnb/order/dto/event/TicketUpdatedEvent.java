package com.fnb.order.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketUpdatedEvent {
    private UUID orderId;
    private UUID ticketId;
    private UUID itemId;
    private String sessionToken;
    private String status;
    private Integer tableNumber;
    private String type; // TICKET hoặc ITEM
    private LocalDateTime updatedAt;
    /** "DINE_IN", "TAKEAWAY", "DELIVERY" */
    private String orderType;
    /** VD: "Bàn 12" hoặc "Mang Đi #001a" */
    private String orderIdentifier;

    private List<CancelledItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelledItem {
        private UUID orderLineItemId;
        private UUID menuItemId;
        private int quantity;
        private String kitchenStatus;
        private List<CancelledOption> options;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelledOption {
        private UUID menuItemId;
        private String optionName;
    }
}
