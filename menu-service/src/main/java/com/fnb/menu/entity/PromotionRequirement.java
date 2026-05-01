package com.fnb.menu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "promotion_requirements", schema = "menu")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Column(name = "min_order_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "min_quantity")
    @Builder.Default
    private int minQuantity = 0;

    /** Ví dụ: GOLD, PLATINUM — null = áp dụng mọi khách */
    @Column(name = "member_level", length = 50)
    private String memberLevel;
}
