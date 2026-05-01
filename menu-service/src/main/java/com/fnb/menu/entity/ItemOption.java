package com.fnb.menu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "item_options", schema = "menu")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private ItemOptionGroup group;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "extra_price", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal extraPrice = BigDecimal.ZERO;

    @Column(name = "is_available")
    @Builder.Default
    private boolean isAvailable = true;
}
