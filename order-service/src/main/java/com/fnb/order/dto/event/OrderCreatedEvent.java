package com.fnb.order.dto.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OrderCreatedEvent {
    private UUID orderId;
    private UUID ticketId;
    private Integer tableNumber;
    private String sessionToken;
    private String note;
    private LocalDateTime createdAt;
    private List<OrderCreatedItemEvent> items;
}
