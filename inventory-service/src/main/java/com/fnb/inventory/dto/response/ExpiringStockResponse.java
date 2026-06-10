package com.fnb.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpiringStockResponse {
    private UUID itemId;
    private String itemName;
    private String itemSku;
    private String lotNumber;
    private LocalDate expiryDate;
    private BigDecimal currentStock;
    private String uomName;
    private long daysRemaining;
    private String status; // "EXPIRED" or "EXPIRING"
    private UUID categoryId;
    private String categoryName;
    private BigDecimal avgCostPrice;
}
