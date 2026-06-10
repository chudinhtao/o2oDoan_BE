package com.fnb.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.fnb.inventory.enums.RecipeType;

@Entity
@Table(name = "recipes", schema = "inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = "items")
public class Recipe extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sale_item_id")
    private UUID saleItemId; // References menu_items.id in Menu Service (loose coupling)

    @Column(name = "modifier_id")
    private UUID modifierId; // References modifier/topping ID in Menu Service (if applicable)

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private RecipeType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_location_id")
    private Location defaultLocation;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RecipeItem> items = new ArrayList<>();
}
