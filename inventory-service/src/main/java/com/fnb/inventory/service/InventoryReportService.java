package com.fnb.inventory.service;

import com.fnb.inventory.dto.response.*;
import com.fnb.inventory.entity.*;
import com.fnb.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fnb.inventory.enums.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fnb.common.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryReportService {

    private final InventoryLevelRepository levelRepository;
    private final StockTransactionRepository transactionRepository;
    private final PurchaseOrderRepository poRepository;
    private final RecipeRepository recipeRepository;
    private final InventoryItemRepository itemRepository;

    @Transactional(readOnly = true)
    public PageResponse<LowStockItemResponse> getLowStockItems(int page, int size, boolean unpaged) {
        Pageable pageable = unpaged ? Pageable.unpaged() : PageRequest.of(page, size);
        Page<Object[]> results = levelRepository.findLowStockItemsWithTotalPageable(pageable);
        List<LowStockItemResponse> content = results.getContent().stream().map(row -> {
            InventoryItem item = (InventoryItem) row[0];
            BigDecimal totalStock = (BigDecimal) row[1];
            if (totalStock == null) totalStock = BigDecimal.ZERO;

            return LowStockItemResponse.builder()
                    .itemId(item.getId())
                    .itemName(item.getName())
                    .itemSku(item.getSku())
                    .uomName(item.getBaseUom() != null ? item.getBaseUom().getName() : "")
                    .currentStock(totalStock)
                    .safetyStock(item.getSafetyStock())
                    .reorderAmount(item.getSafetyStock().subtract(totalStock).max(BigDecimal.ZERO))
                    .categoryId(item.getCategory() != null ? item.getCategory().getId() : null)
                    .categoryName(item.getCategory() != null ? item.getCategory().getName() : "")
                    .avgCostPrice(item.getAvgCostPrice() != null ? item.getAvgCostPrice() : BigDecimal.ZERO)
                    .build();
        }).collect(Collectors.toList());
        return PageResponse.of(content, unpaged ? 0 : page, unpaged ? content.size() : size, results.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpiringStockResponse> getExpiringStockItems(int daysThreshold, int page, int size, boolean unpaged) {
        java.time.LocalDate targetDate = java.time.LocalDate.now().plusDays(daysThreshold);
        Pageable pageable = unpaged ? Pageable.unpaged() : PageRequest.of(page, size);
        Page<InventoryLevel> expiringLevels = levelRepository.findExpiringItemsPageable(targetDate, pageable);
        List<ExpiringStockResponse> content = expiringLevels.getContent().stream().map(level -> {
            InventoryItem item = level.getItem();
            long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), level.getBatch().getExpiryDate());
            String status = daysRemaining < 0 ? "EXPIRED" : "EXPIRING";
            return ExpiringStockResponse.builder()
                    .itemId(item.getId())
                    .itemName(item.getName())
                    .itemSku(item.getSku())
                    .lotNumber(level.getBatch().getLotNumber())
                    .expiryDate(level.getBatch().getExpiryDate())
                    .currentStock(level.getCurrentStock())
                    .uomName(item.getBaseUom() != null ? item.getBaseUom().getName() : "")
                    .daysRemaining(daysRemaining)
                    .status(status)
                    .categoryId(item.getCategory() != null ? item.getCategory().getId() : null)
                    .categoryName(item.getCategory() != null ? item.getCategory().getName() : "")
                    .avgCostPrice(item.getAvgCostPrice() != null ? item.getAvgCostPrice() : BigDecimal.ZERO)
                    .build();
        }).collect(Collectors.toList());
        return PageResponse.of(content, unpaged ? 0 : page, unpaged ? content.size() : size, expiringLevels.getTotalElements());
    }

    @Transactional(readOnly = true)
    public VarianceReportResponse getVarianceReport(LocalDateTime startDate, LocalDateTime endDate) {
        List<StockTransaction> transactions = transactionRepository.findVarianceTransactions(startDate, endDate);

        // Group by Item ID
        Map<UUID, List<StockTransaction>> groupedTx = transactions.stream()
                .collect(Collectors.groupingBy(tx -> tx.getItem().getId()));

        BigDecimal totalEstimatedLoss = BigDecimal.ZERO;
        List<VarianceReportItemResponse> reportItems = groupedTx.entrySet().stream().map(entry -> {
            UUID itemId = entry.getKey();
            List<StockTransaction> itemTxs = entry.getValue();
            InventoryItem item = itemTxs.get(0).getItem();

            BigDecimal wasteQty = BigDecimal.ZERO;
            BigDecimal adjustQty = BigDecimal.ZERO;

            for (StockTransaction tx : itemTxs) {
                if (tx.getTransactionType() == TransactionType.OUT_WASTE) {
                    wasteQty = wasteQty.add(tx.getQuantityChange());
                } else if (tx.getTransactionType() == TransactionType.ADJUSTMENT) {
                    adjustQty = adjustQty.add(tx.getQuantityChange());
                }
            }

            BigDecimal negativeAdjustQty = adjustQty.compareTo(BigDecimal.ZERO) < 0 ? adjustQty : BigDecimal.ZERO;
            BigDecimal totalLossQty = wasteQty.add(negativeAdjustQty);
            BigDecimal totalVarianceQty = wasteQty.add(adjustQty);
            BigDecimal itemAvgCost = item.getAvgCostPrice() != null ? item.getAvgCostPrice() : BigDecimal.ZERO;
            BigDecimal lossValue = BigDecimal.ZERO;
            if (totalLossQty.compareTo(BigDecimal.ZERO) < 0) {
                lossValue = totalLossQty.abs().multiply(itemAvgCost);
            }

            return VarianceReportItemResponse.builder()
                    .itemId(itemId)
                    .itemName(item.getName())
                    .itemSku(item.getSku())
                    .uomName(item.getBaseUom() != null ? item.getBaseUom().getName() : "")
                    .wasteQuantity(wasteQty)
                    .adjustmentQuantity(adjustQty)
                    .totalVarianceQuantity(totalVarianceQty)
                    .estimatedLossValue(lossValue)
                    .categoryId(item.getCategory() != null ? item.getCategory().getId() : null)
                    .categoryName(item.getCategory() != null ? item.getCategory().getName() : "")
                    .build();
        }).collect(Collectors.toList());

        // Sort by highest loss value first
        reportItems.sort((a, b) -> b.getEstimatedLossValue().compareTo(a.getEstimatedLossValue()));

        for (VarianceReportItemResponse item : reportItems) {
            totalEstimatedLoss = totalEstimatedLoss.add(item.getEstimatedLossValue());
        }

        return VarianceReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalEstimatedLossValue(totalEstimatedLoss)
                .items(reportItems)
                .build();
    }



    @Transactional(readOnly = true)
    public List<InventorySuggestionResponse> getPurchaseSuggestions() {
        List<Object[]> results = levelRepository.findLowStockItemsWithTotal();
        
        return results.stream().map(row -> {
            InventoryItem item = (InventoryItem) row[0];
            BigDecimal totalStock = (BigDecimal) row[1];
            if (totalStock == null) totalStock = BigDecimal.ZERO;
            
            // Suggestion logic: (Safety Stock * 2) - Current Stock
            BigDecimal suggested = item.getSafetyStock().multiply(BigDecimal.valueOf(2))
                    .subtract(totalStock)
                    .max(BigDecimal.ZERO);
            
            return InventorySuggestionResponse.builder()
                    .itemId(item.getId())
                    .itemName(item.getName())
                    .itemSku(item.getSku())
                    .currentStock(totalStock)
                    .safetyStock(item.getSafetyStock())
                    .suggestedQuantity(suggested)
                    .supplierId(null)
                    .supplierName("Chọn NCC")
                    .uomName(item.getBaseUom() != null ? item.getBaseUom().getName() : "")
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime now = LocalDateTime.now();
        if (startDate == null) {
            startDate = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        }
        if (endDate == null) {
            endDate = now;
        }

        BigDecimal totalValuation = levelRepository.calculateTotalInventoryValue();
        long lowStockCount = levelRepository.findLowStockItemsWithTotal().size();
        long expiringCount = levelRepository.findExpiringItems(java.time.LocalDate.now().plusDays(7)).size();
        
        BigDecimal cogs = java.util.Optional.ofNullable(transactionRepository.calculateCogs(startDate, endDate)).orElse(BigDecimal.ZERO);
        BigDecimal waste = java.util.Optional.ofNullable(transactionRepository.calculateWasteValue(startDate, endDate)).orElse(BigDecimal.ZERO);
        
        long pendingPos = poRepository.findAll().stream()
                .filter(po -> POStatus.DRAFT.equals(po.getStatus()) 
                           || POStatus.CONFIRMED.equals(po.getStatus())
                           || POStatus.PARTIAL_RECEIVED.equals(po.getStatus()))
                .count();

        return DashboardSummaryResponse.builder()
                .totalInventoryValue(totalValuation)
                .lowStockCount(lowStockCount)
                .expiringItemsCount(expiringCount)
                .cogsThisMonth(cogs)
                .wasteValueThisMonth(waste)
                .pendingPurchaseOrders(pendingPos)
                .build();
    }

    @Transactional(readOnly = true)
    public List<InventoryTrendResponse> getTrendData(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(7).withHour(0).withMinute(0).withSecond(0);
        }
        return transactionRepository.calculateTrend(startDate).stream()
                .map(p -> InventoryTrendResponse.builder()
                        .date(p.getDate())
                        .cogs(p.getCogs())
                        .waste(p.getWaste())
                        .build())
                .collect(Collectors.toList());
    }
}
