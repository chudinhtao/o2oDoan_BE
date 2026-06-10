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
public class StocktakeResponse {
    private UUID id;
    private com.fnb.inventory.enums.StocktakeStatus status;
    private String name;
    private String notes;
    private LocalDateTime snapshotTime;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private UUID locationId;
    private String locationName;
    private List<StocktakeItemResponse> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StocktakeItemResponse {
        private UUID id;
        private UUID itemId;
        private String itemName;
        private String itemSku;
        private BigDecimal systemQuantity;
        private BigDecimal countedQuantity;
        private BigDecimal variance;
        private String adjustmentReason;
        private UUID batchId;
        private String lotNumber;
        private java.time.LocalDate expiryDate;
    }
}
