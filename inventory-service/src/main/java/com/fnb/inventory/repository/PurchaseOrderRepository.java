package com.fnb.inventory.repository;

import com.fnb.inventory.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fnb.inventory.enums.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    @Query(value = """
        SELECT po.* FROM inventory.purchase_orders po
        LEFT JOIN inventory.suppliers s ON s.id = po.supplier_id
        WHERE (CAST(:status AS text) IS NULL OR po.status = CAST(:status AS text))
        AND (CAST(:type AS text) IS NULL OR po.type = CAST(:type AS text))
        AND (CAST(:poNumber AS text) IS NULL OR LOWER(po.po_number) LIKE LOWER(CONCAT('%', CAST(:poNumber AS text), '%'))
             OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:poNumber AS text), '%')))
        AND (CAST(:startDate AS timestamp) IS NULL OR po.created_at >= CAST(:startDate AS timestamp))
        AND (CAST(:endDate AS timestamp) IS NULL OR po.created_at <= CAST(:endDate AS timestamp))
        ORDER BY po.created_at DESC NULLS LAST, po.po_number DESC
    """, countQuery = """
        SELECT count(po.id) FROM inventory.purchase_orders po
        LEFT JOIN inventory.suppliers s ON s.id = po.supplier_id
        WHERE (CAST(:status AS text) IS NULL OR po.status = CAST(:status AS text))
        AND (CAST(:type AS text) IS NULL OR po.type = CAST(:type AS text))
        AND (CAST(:poNumber AS text) IS NULL OR LOWER(po.po_number) LIKE LOWER(CONCAT('%', CAST(:poNumber AS text), '%'))
             OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:poNumber AS text), '%')))
        AND (CAST(:startDate AS timestamp) IS NULL OR po.created_at >= CAST(:startDate AS timestamp))
        AND (CAST(:endDate AS timestamp) IS NULL OR po.created_at <= CAST(:endDate AS timestamp))
    """, nativeQuery = true)
    Page<PurchaseOrder> findWithFilter(
            @Param("status") String status,
            @Param("type") String type,
            @Param("poNumber") String poNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}
