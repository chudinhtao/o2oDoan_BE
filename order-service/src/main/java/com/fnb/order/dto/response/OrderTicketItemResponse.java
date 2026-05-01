package com.fnb.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OrderTicketItemResponse {
    private UUID id;
    private UUID menuItemId;
    private String itemName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private String note;
    private String status;
    private String station;
    private LocalDateTime createdAt;
    private List<OrderItemOptionResponse> options;
}
