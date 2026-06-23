package com.fnb.ai.feign;

import com.fnb.common.dto.ApiResponse;
import com.fnb.common.dto.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@FeignClient(name = "inventory-service")
public interface InventoryFeignClient {

    @GetMapping("/api/admin/inventory/reports/low-stock")
    ApiResponse<PageResponse<LowStockItemRow>> getLowStockItems(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("unpaged") boolean unpaged);

    @GetMapping("/api/admin/inventory/reports/expiring")
    ApiResponse<PageResponse<ExpiringStockRow>> getExpiringStockItems(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("unpaged") boolean unpaged);

    @GetMapping("/api/admin/inventory/reports/variance")
    ApiResponse<VarianceReportRow> getVarianceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate);

    @GetMapping("/api/admin/inventory/reports/dashboard-summary")
    ApiResponse<DashboardSummaryRow> getDashboardSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate);

    @GetMapping("/api/admin/inventory/reports/suggestions")
    ApiResponse<List<InventorySuggestionRow>> getPurchaseSuggestions();

    // ─── Response Records ─────────────────────────────────────────────────────

    record LowStockItemRow(
            UUID itemId,
            String itemName,
            String itemSku,
            String uomName,
            BigDecimal currentStock,
            BigDecimal safetyStock,
            BigDecimal reorderAmount,
            UUID categoryId,
            String categoryName,
            BigDecimal avgCostPrice
    ) {}

    record ExpiringStockRow(
            UUID itemId,
            String itemName,
            String itemSku,
            String lotNumber,
            LocalDate expiryDate,
            BigDecimal currentStock,
            String uomName,
            long daysRemaining,
            String status,
            UUID categoryId,
            String categoryName,
            BigDecimal avgCostPrice
    ) {}

    record VarianceReportRow(
            LocalDateTime startDate,
            LocalDateTime endDate,
            BigDecimal totalEstimatedLossValue,
            List<VarianceItemRow> items
    ) {}

    record VarianceItemRow(
            String itemName,
            BigDecimal expectedStock,
            BigDecimal actualStock,
            BigDecimal variance,
            BigDecimal estimatedLossValue
    ) {}

    record DashboardSummaryRow(
            BigDecimal totalInventoryValue,
            long lowStockCount,
            long expiringItemsCount,
            BigDecimal cogsThisMonth,
            BigDecimal wasteValueThisMonth,
            long pendingPurchaseOrders
    ) {}

    record InventorySuggestionRow(
            UUID itemId,
            String itemName,
            String itemSku,
            BigDecimal currentStock,
            BigDecimal safetyStock,
            BigDecimal suggestedQuantity,
            UUID supplierId,
            String supplierName,
            String uomName
    ) {}

    // Deep Inventory APIs (Phase 6)
    @GetMapping("/api/admin/inventory/purchase-orders")
    ApiResponse<Object> getPurchaseOrders(
            @RequestParam(required = false, value = "status") String status,
            @RequestParam(required = false, value = "startDate") String startDate,
            @RequestParam(required = false, value = "endDate") String endDate,
            @RequestParam(defaultValue = "0", value = "page") int page,
            @RequestParam(defaultValue = "20", value = "size") int size
    );

    @GetMapping("/api/admin/inventory/stocktakes")
    ApiResponse<Object> getStocktakes(
            @RequestParam(required = false, value = "status") String status,
            @RequestParam(required = false, value = "startDate") String startDate,
            @RequestParam(required = false, value = "endDate") String endDate,
            @RequestParam(defaultValue = "0", value = "page") int page,
            @RequestParam(defaultValue = "20", value = "size") int size
    );

    @GetMapping("/api/admin/inventory/transactions")
    ApiResponse<Object> getStockTransactions(
            @RequestParam(required = false, value = "itemId") UUID itemId,
            @RequestParam(required = false, value = "transactionType") String type,
            @RequestParam(required = false, value = "startDate") String startDate,
            @RequestParam(required = false, value = "endDate") String endDate,
            @RequestParam(defaultValue = "0", value = "page") int page,
            @RequestParam(defaultValue = "20", value = "size") int size
    );

    // Deep Inventory APIs (Phase 9 - Recipes & Suppliers)
    @GetMapping("/api/admin/inventory/suppliers")
    ApiResponse<Object> getSuppliers(
            @RequestParam(required = false, value = "search") String search,
            @RequestParam(defaultValue = "0", value = "page") int page,
            @RequestParam(defaultValue = "50", value = "size") int size
    );

    @GetMapping("/api/admin/inventory/recipes/by-sale-item/{saleItemId}")
    ApiResponse<Object> getRecipeBySaleItem(
            @org.springframework.web.bind.annotation.PathVariable("saleItemId") UUID saleItemId
    );
}
