package com.fnb.inventory.dto.request;

import jakarta.validation.constraints.Min;
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
public class PurchaseOrderItemRequest {
    @NotNull(message = "Item ID is required")
    private UUID itemId;

    @NotNull(message = "Ordered quantity is required")
    @Min(value = 0, message = "Ordered quantity must be positive")
    private BigDecimal orderedQuantity;

    @NotNull(message = "UoM ID is required")
    private UUID uomId;

    @NotNull(message = "Unit price is required")
    @Min(value = 0, message = "Unit price must be non-negative")
    private BigDecimal unitPrice;

    private String batchNumber;

    private java.time.LocalDate expiryDate;
}
