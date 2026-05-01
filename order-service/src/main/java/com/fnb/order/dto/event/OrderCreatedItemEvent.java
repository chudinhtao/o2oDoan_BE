package com.fnb.order.dto.event;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OrderCreatedItemEvent {
    private UUID menuItemId;
    private String itemName;
    private int quantity;
    private String note;
    private String station;
    private BigDecimal unitPrice;
    private List<OrderCreatedOptionEvent> options;
}
