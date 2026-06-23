package com.fnb.inventory.repository;

import com.fnb.inventory.entity.StockTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;
import com.fnb.inventory.enums.TransactionType;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, UUID> {

    boolean existsByReferenceIdAndOrderLineItemIdAndItem_IdAndTransactionType(
            UUID referenceId, UUID orderLineItemId, UUID itemId, TransactionType transactionType);

    boolean existsByOrderLineItemIdAndTransactionType(UUID orderLineItemId, TransactionType transactionType);

    @Query("SELECT st FROM StockTransaction st WHERE st.orderLineItemId = :orderLineItemId AND st.item.id = :itemId AND st.transactionType = :transactionType")
    java.util.List<StockTransaction> findByOrderLineItemIdAndItemIdAndTransactionType(
            @Param("orderLineItemId") UUID orderLineItemId, 
            @Param("itemId") UUID itemId, 
            @Param("transactionType") TransactionType transactionType);

    @Query(value = """
        SELECT st.* FROM inventory.stock_transactions st
        WHERE (CAST(:itemId AS uuid) IS NULL OR st.item_id = CAST(:itemId AS uuid))
        AND (CAST(:transactionType AS text) IS NULL OR st.transaction_type = CAST(:transactionType AS text))
        AND (CAST(:startDate AS timestamp) IS NULL OR st.created_at >= CAST(:startDate AS timestamp))
        AND (CAST(:endDate AS timestamp) IS NULL OR st.created_at <= CAST(:endDate AS timestamp))
        ORDER BY st.created_at DESC
    """, countQuery = """
        SELECT count(st.id) FROM inventory.stock_transactions st
        WHERE (CAST(:itemId AS uuid) IS NULL OR st.item_id = CAST(:itemId AS uuid))
        AND (CAST(:transactionType AS text) IS NULL OR st.transaction_type = CAST(:transactionType AS text))
        AND (CAST(:startDate AS timestamp) IS NULL OR st.created_at >= CAST(:startDate AS timestamp))
        AND (CAST(:endDate AS timestamp) IS NULL OR st.created_at <= CAST(:endDate AS timestamp))
    """, nativeQuery = true)
    Page<StockTransaction> findWithFilter(
            @Param("itemId") UUID itemId,
            @Param("transactionType") String transactionType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("""
        SELECT s FROM StockTransaction s
        JOIN FETCH s.item i
        WHERE s.transactionType IN (com.fnb.inventory.enums.TransactionType.OUT_WASTE, com.fnb.inventory.enums.TransactionType.ADJUSTMENT)
        AND s.createdAt >= :startDate
        AND s.createdAt <= :endDate
    """)
    java.util.List<StockTransaction> findVarianceTransactions(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(value = """
        SELECT COALESCE(ABS(SUM(st.quantity_change * COALESCE(st.unit_price_at_transaction, i.avg_cost_price, 0))), 0)
        FROM inventory.stock_transactions st
        JOIN inventory.inventory_items i ON st.item_id = i.id
        WHERE st.transaction_type = 'OUT_SALE'
        AND st.created_at >= :startDate
        AND st.created_at <= :endDate
    """, nativeQuery = true)
    java.math.BigDecimal calculateCogs(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = """
        SELECT COALESCE(ABS(SUM(st.quantity_change * COALESCE(st.unit_price_at_transaction, i.avg_cost_price, 0))), 0)
        FROM inventory.stock_transactions st
        JOIN inventory.inventory_items i ON st.item_id = i.id
        WHERE (st.transaction_type = 'OUT_WASTE' OR (st.transaction_type = 'ADJUSTMENT' AND st.quantity_change < 0))
        AND st.created_at >= :startDate
        AND st.created_at <= :endDate
    """, nativeQuery = true)
    java.math.BigDecimal calculateWasteValue(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = """
        SELECT 
            CAST(st.created_at AS DATE) as date,
            COALESCE(ABS(SUM(CASE WHEN st.transaction_type = 'OUT_SALE' THEN st.quantity_change * COALESCE(st.unit_price_at_transaction, i.avg_cost_price, 0) ELSE 0 END)), 0)::numeric as cogs,
            COALESCE(ABS(SUM(CASE WHEN (st.transaction_type = 'OUT_WASTE' OR (st.transaction_type = 'ADJUSTMENT' AND st.quantity_change < 0)) THEN st.quantity_change * COALESCE(st.unit_price_at_transaction, i.avg_cost_price, 0) ELSE 0 END)), 0)::numeric as waste
        FROM inventory.stock_transactions st
        JOIN inventory.inventory_items i ON st.item_id = i.id
        WHERE st.created_at >= :startDate
        GROUP BY CAST(st.created_at AS DATE)
        ORDER BY CAST(st.created_at AS DATE) ASC
    """, nativeQuery = true)
    java.util.List<com.fnb.inventory.repository.StockTransactionRepository.InventoryTrendProjection> calculateTrend(@Param("startDate") LocalDateTime startDate);

    interface InventoryTrendProjection {
        java.time.LocalDate getDate();
        java.math.BigDecimal getCogs();
        java.math.BigDecimal getWaste();
    }
}
