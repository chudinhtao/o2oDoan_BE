package com.fnb.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderResponse {
    private UUID id;
    private String poNumber;
    private UUID supplierId;
    private String supplierName;
    private com.fnb.inventory.enums.POType type;
    private com.fnb.inventory.enums.POStatus status;
    private BigDecimal totalAmount;
    private java.time.LocalDate expectedDate;
    private String notes;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime confirmedAt;
    private List<PurchaseOrderItemResponse> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseOrderItemResponse {
        private UUID id;
        private UUID itemId;
        private String itemName;
        private String itemSku;
        private BigDecimal orderedQuantity;
        private BigDecimal receivedQuantity;
        private BigDecimal remainingQuantity;
        private UUID uomId;
        private String uomName;
        private BigDecimal unitPrice;
        private BigDecimal totalLineAmount;
        private String batchNumber;
        private java.time.LocalDate expiryDate;
    }
}
