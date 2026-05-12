package com.fnb.order.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdatedEvent {
    private UUID orderId;
    private String status;
    private String sessionToken;
    private String tableNumber;
    /** "DINE_IN", "TAKEAWAY", "DELIVERY" */
    private String orderType;
    /** VD: "Bàn 12" hoặc "Mang Đi #001a" */
    private String orderIdentifier;
}
