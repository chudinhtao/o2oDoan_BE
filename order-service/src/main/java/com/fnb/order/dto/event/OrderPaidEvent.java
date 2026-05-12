	package com.fnb.order.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaidEvent {
    private UUID orderId;
    private UUID tableId;
    private Integer tableNumber;
    private String sessionToken;
    private LocalDateTime paidAt;
    /** "DINE_IN", "TAKEAWAY", "DELIVERY" */
    private String orderType;
    /** VD: "Bàn 12" hoặc "Mang Đi #001a" */
    private String orderIdentifier;
}
