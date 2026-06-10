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
    private String orderIdentifier; // VD: "Bàn 12" hoặc "Mang Đi #A1B2"
    private String status; // OPEN, PAID, CANCELLED
    private String source; // QR, MANUAL
    private String orderType; // DINE_IN, TAKEAWAY, DELIVERY
    private String customerName;
    private String customerPhone;
    private BigDecimal subtotal;
    private BigDecimal depositAmount;
    private BigDecimal discount;
    private BigDecimal total;
    private UUID promotionId;
    private String promotionCode;
    private String discountType;
    private BigDecimal discountRate;
    private BigDecimal tax;
    private BigDecimal serviceFee;
    private String paymentMethod;
    private String paymentDetail; // JSON string for MIXED payment or extra payment data
    private Long payosOrderCode;
    private LocalDateTime paidAt;
    private UUID cashierId;
    private UUID cancelledBy;
    private String cancelReason;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountValue;
    private Boolean isStackable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderTicketResponse> tickets;
    private List<OrderSummaryItemResponse> summaryItems;
}
