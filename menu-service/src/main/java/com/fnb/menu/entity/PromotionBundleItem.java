package com.fnb.menu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "promotion_bundle_items", schema = "menu")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionBundleItem implements com.fnb.common.dto.IBundleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(nullable = false)
    @Builder.Default
    private int quantity = 1;

    /** BUY = điều kiện mua, GET = tặng/giảm */
    @Column(nullable = false, length = 10)
    private String role;
}
