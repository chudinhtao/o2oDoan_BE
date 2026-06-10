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
public class InventoryBatchResponse {
    private UUID id;
    private String lotNumber;
    private LocalDate expiryDate;
    private BigDecimal currentStock;
    private UUID locationId;
    private String locationName;
}
