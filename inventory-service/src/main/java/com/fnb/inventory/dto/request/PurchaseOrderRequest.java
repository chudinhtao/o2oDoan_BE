package com.fnb.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderRequest {
    private UUID supplierId; // Can be null for quick GRN

    @NotNull(message = "PO Type is required (STANDARD, QUICK_GRN)")
    private com.fnb.inventory.enums.POType type;

    private java.time.LocalDate expectedDate;
    private String notes;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<PurchaseOrderItemRequest> items;

    private UUID locationId;
}
