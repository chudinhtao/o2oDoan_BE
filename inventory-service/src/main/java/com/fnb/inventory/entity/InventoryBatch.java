package com.fnb.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Quản lý Lô và Hạn sử dụng của nguyên liệu theo tiêu chuẩn F&B (FIFO/FEFO).
 */
@Entity
@Table(name = "inventory_batches", schema = "inventory",
        indexes = @Index(name = "idx_inventory_batches_expiry", columnList = "item_id, expiry_date"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class InventoryBatch extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Column(name = "lot_number", length = 100)
    private String lotNumber;

    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;
}
