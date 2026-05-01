package com.fnb.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OrderResponse {
    private UUID id;
    private UUID sessionId;
    private UUID tableId;
    private String tableNumber;
    private String status; // OPEN, PAID, CANCELLED
    private String source; // QR, MANUAL
    private String orderType; // DINE_IN, TAKEAWAY, DELIVERY
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
    private UUID promotionId;
    private String promotionCode;
    private LocalDateTime createdAt;
    private List<OrderTicketResponse> tickets;
    private List<OrderSummaryItemResponse> summaryItems;
}
