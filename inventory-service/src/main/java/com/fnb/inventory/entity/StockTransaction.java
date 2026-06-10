package com.fnb.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.fnb.inventory.enums.TransactionType;

/**
 * Sổ cái kho - BẤT KHẢ XÂM PHẠM (Immutable audit trail).
 * Không extends BaseAuditEntity vì chỉ có created_at + created_by, KHÔNG CÓ updated.
 * Rule: KHÔNG bao giờ UPDATE hoặc DELETE bản ghi trong bảng này.
 */
@Entity
@Table(name = "stock_transactions", schema = "inventory",
        indexes = @Index(name = "idx_stock_transactions_item_date", columnList = "item_id, created_at"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private InventoryBatch batch;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 30, nullable = false)
    private TransactionType transactionType;

    @Column(name = "quantity_change", precision = 15, scale = 4, nullable = false)
    private BigDecimal quantityChange;

    @Column(name = "unit_price_at_transaction", precision = 15, scale = 2)
    private BigDecimal unitPriceAtTransaction;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "order_line_item_id")
    private UUID orderLineItemId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;
}
