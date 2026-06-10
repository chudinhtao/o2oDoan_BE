package com.fnb.inventory.repository;

import com.fnb.inventory.entity.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.fnb.inventory.enums.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    boolean existsBySku(String sku);

    @Query(value = """
        SELECT i.* FROM inventory.inventory_items i
        LEFT JOIN inventory.item_categories c ON c.id = i.category_id
        LEFT JOIN inventory.uoms u ON u.id = i.base_uom_id
        WHERE (CAST(:categoryId AS uuid) IS NULL OR i.category_id = CAST(:categoryId AS uuid))
        AND (CAST(:type AS text) IS NULL OR i.type = CAST(:type AS text))
        AND (CAST(:isActive AS boolean) IS NULL OR i.is_active = CAST(:isActive AS boolean))
        AND (CAST(:keyword AS text) IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))
             OR LOWER(i.sku) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')))
    """, countQuery = """
        SELECT count(i.id) FROM inventory.inventory_items i
        WHERE (CAST(:categoryId AS uuid) IS NULL OR i.category_id = CAST(:categoryId AS uuid))
        AND (CAST(:type AS text) IS NULL OR i.type = CAST(:type AS text))
        AND (CAST(:isActive AS boolean) IS NULL OR i.is_active = CAST(:isActive AS boolean))
        AND (CAST(:keyword AS text) IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))
             OR LOWER(i.sku) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')))
    """, nativeQuery = true)
    Page<InventoryItem> findAllWithFilter(
            @Param("categoryId") UUID categoryId,
            @Param("type") String type,
            @Param("isActive") Boolean isActive,
            @Param("keyword") String keyword,
            Pageable pageable);
}
