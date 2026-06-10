package com.fnb.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;
import com.fnb.inventory.enums.ItemType;

@Entity
@Table(name = "inventory_items", schema = "inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class InventoryItem extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 50, unique = true)
    private String sku;

    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ItemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ItemType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_uom_id")
    private Uom baseUom;

    @Column(name = "safety_stock", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal safetyStock = BigDecimal.ZERO;

    @Column(name = "avg_cost_price", precision = 15, scale = 2)
    private BigDecimal avgCostPrice;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;
}
