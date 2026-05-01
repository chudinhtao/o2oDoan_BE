package com.fnb.menu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "promotion_targets", schema = "menu")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionTarget implements com.fnb.common.dto.ITarget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    /** ITEM | CATEGORY | GLOBAL */
    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    /** UUID của item/category, null nếu GLOBAL */
    @Column(name = "target_id")
    private UUID targetId;
}
