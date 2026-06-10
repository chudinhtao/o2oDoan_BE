package com.fnb.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "purchase_order_items", schema = "inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PurchaseOrderItem extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    /** Cột cũ (Deprecated nhưng DB vẫn còn NOT NULL) - Đồng bộ với orderedQuantity */
    @Column(name = "quantity", precision = 10, scale = 4)
    private BigDecimal quantity;

    /** Số lượng đặt hàng gốc (Locked sau khi CONFIRMED) */
    @Column(name = "ordered_quantity", precision = 10, scale = 4)
    private BigDecimal orderedQuantity;

    /** Số lượng đã thực nhận (cộng dồn qua các lần nhận hàng) */
    @Column(name = "received_quantity", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal receivedQuantity = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id", nullable = false)
    private Uom uom;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "batch_number", length = 50)
    private String batchNumber;

    @Column(name = "expiry_date")
    private java.time.LocalDate expiryDate;

    /** Tính toán số lượng còn thiếu */
    public BigDecimal getRemainingQuantity() {
        BigDecimal ord = orderedQuantity != null ? orderedQuantity : BigDecimal.ZERO;
        BigDecimal rec = receivedQuantity != null ? receivedQuantity : BigDecimal.ZERO;
        return ord.subtract(rec).max(BigDecimal.ZERO);
    }

    public BigDecimal getOrderedQuantity() {
        return orderedQuantity != null ? orderedQuantity : BigDecimal.ZERO;
    }

    public BigDecimal getReceivedQuantity() {
        return receivedQuantity != null ? receivedQuantity : BigDecimal.ZERO;
    }

    @PrePersist
    @PreUpdate
    public void syncLegacyQuantity() {
        if (this.orderedQuantity != null) {
            this.quantity = this.orderedQuantity;
        }
    }
}
