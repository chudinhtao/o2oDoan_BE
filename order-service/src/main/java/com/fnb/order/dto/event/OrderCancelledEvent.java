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
public class OrderCancelledEvent {
    private UUID orderId;
    private UUID tableId;
    private Integer tableNumber;
    private String sessionToken;
    private LocalDateTime cancelledAt;
    private String orderType;
    private String orderIdentifier;
    private String cancelReason;
    private UUID cancelledBy;

    private List<OrderPaidEvent.LineItem> lineItems;
}
