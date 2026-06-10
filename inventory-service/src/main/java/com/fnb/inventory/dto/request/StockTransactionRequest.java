package com.fnb.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransactionRequest {
    @NotNull(message = "Item ID is required")
    private UUID itemId;

    private UUID locationId;

    @NotNull(message = "Transaction type is required")
    private com.fnb.inventory.enums.TransactionType transactionType; // IN_PO, IN_QUICK, OUT_SALE, OUT_WASTE, ADJUSTMENT

    @NotNull(message = "Quantity change is required")
    private BigDecimal quantityChange; // Positive for IN, Negative for OUT

    private BigDecimal unitPriceAtTransaction;
    private UUID referenceId; // PO ID, Order ID, etc.
    private UUID orderLineItemId;
    private String reason;
    private String lotNumber;
    private java.time.LocalDate expiryDate;
}
