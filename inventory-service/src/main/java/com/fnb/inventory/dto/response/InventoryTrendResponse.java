package com.fnb.inventory.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class InventoryTrendResponse {
    private LocalDate date;
    private BigDecimal cogs;
    private BigDecimal waste;
}
