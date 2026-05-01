	package com.fnb.order.dto.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OrderPaidEvent {
    private UUID orderId;
    private UUID tableId;
    private Integer tableNumber;
    private String sessionToken;
    private LocalDateTime paidAt;
}
