package com.fnb.inventory.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class DashboardSummaryResponse {
    private BigDecimal totalInventoryValue;
    private long lowStockCount;
    private long expiringItemsCount;
    private BigDecimal cogsThisMonth;
    private BigDecimal wasteValueThisMonth;
    private long pendingPurchaseOrders;
}
