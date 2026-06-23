package com.fnb.inventory.repository;

import com.fnb.inventory.entity.InventoryLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryLevelRepository extends JpaRepository<InventoryLevel, UUID> {
    List<InventoryLevel> findByItemId(UUID itemId);
    
    List<InventoryLevel> findByItemIsActiveTrue();
    
    List<InventoryLevel> findByItemIsActiveTrueAndLocationId(UUID locationId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT i, SUM(l.currentStock) as totalStock
        FROM InventoryItem i
        LEFT JOIN InventoryLevel l ON l.item = i
        WHERE i.isActive = true
        GROUP BY i
        HAVING COALESCE(SUM(l.currentStock), 0) <= i.safetyStock
    """)
    java.util.List<Object[]> findLowStockItemsWithTotal();

    @org.springframework.data.jpa.repository.Query("""
        SELECT i, SUM(l.currentStock) as totalStock
        FROM InventoryItem i
        LEFT JOIN InventoryLevel l ON l.item = i
        WHERE i.isActive = true
        GROUP BY i
        HAVING COALESCE(SUM(l.currentStock), 0) <= i.safetyStock
    """)
    org.springframework.data.domain.Page<Object[]> findLowStockItemsWithTotalPageable(org.springframework.data.domain.Pageable pageable);

    /** @deprecated Use findLowStockItemsWithTotal instead */
    @org.springframework.data.jpa.repository.Query("""
        SELECT l FROM InventoryLevel l
        JOIN FETCH l.item i
        WHERE l.currentStock <= i.safetyStock
        AND i.isActive = true
    """)
    java.util.List<InventoryLevel> findLowStockItems();

    @org.springframework.data.jpa.repository.Query("""
        SELECT l FROM InventoryLevel l
        JOIN FETCH l.item i
        JOIN FETCH l.batch b
        WHERE l.currentStock > 0
        AND b.expiryDate <= :targetDate
        AND i.isActive = true
        ORDER BY b.expiryDate ASC
    """)
    java.util.List<InventoryLevel> findExpiringItems(@org.springframework.data.repository.query.Param("targetDate") java.time.LocalDate targetDate);

    @org.springframework.data.jpa.repository.Query("""
        SELECT l FROM InventoryLevel l
        JOIN FETCH l.item i
        JOIN FETCH l.batch b
        WHERE l.currentStock > 0
        AND b.expiryDate <= :targetDate
        AND i.isActive = true
        ORDER BY b.expiryDate ASC
    """)
    org.springframework.data.domain.Page<InventoryLevel> findExpiringItemsPageable(
            @org.springframework.data.repository.query.Param("targetDate") java.time.LocalDate targetDate,
            org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query(value = """
        SELECT COALESCE(SUM(l.current_stock * COALESCE(i.avg_cost_price, 0)), 0)
        FROM inventory_levels l
        JOIN inventory_items i ON l.item_id = i.id
        WHERE i.is_active = true
    """, nativeQuery = true)
    java.math.BigDecimal calculateTotalInventoryValue();
}
