package com.fnb.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransactionResponse {
    private UUID id;
    private UUID itemId;
    private String itemName;
    private String itemSku;
    private String baseUomName;
    private com.fnb.inventory.enums.TransactionType transactionType;
    private BigDecimal quantityChange;
    private BigDecimal unitPriceAtTransaction;
    private UUID referenceId;
    private UUID orderLineItemId;
    private String reason;
    private LocalDateTime createdAt;
    private String createdBy;
    private String lotNumber;
    private java.time.LocalDate expiryDate;
    private UUID locationId;
    private String locationName;
}
