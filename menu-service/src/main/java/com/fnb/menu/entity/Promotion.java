package com.fnb.menu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Sub-entity imports (same package — explicit for Lombok/JPA clarity)


@Entity
@Table(name = "promotions", schema = "menu")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"targets", "bundleItems", "requirements", "schedules"})
public class Promotion implements com.fnb.common.dto.IBundleRule {

    // Interface fulfillment
    @Override
    public List<? extends com.fnb.common.dto.IBundleItem> getBundleItems() {
        return this.bundleItems;
    }

    // Entity-specific access for modifications
    public List<PromotionBundleItem> getEntityBundleItems() {
        return this.bundleItems;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Null nếu trigger_type = AUTO */
    @Column(unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    /** PRODUCT | ORDER | BUNDLE */
    @Column(nullable = false, length = 20)
    private String scope;

    /** AUTO | COUPON */
    @Column(name = "trigger_type", nullable = false, length = 20)
    private String triggerType;

    /** PERCENT | FIX_AMOUNT | FIX_PRICE */
    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType;

    @Column(name = "discount_value", precision = 12, scale = 2)
    private BigDecimal discountValue;

    /** Giới hạn mức giảm tối đa — dùng khi discountType = PERCENT */
    @Column(name = "max_discount", precision = 12, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count")
    @Builder.Default
    private int usedCount = 0;

    @Column
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    /** FALSE = không được dùng chung với KM khác */
    @Column(name = "is_stackable")
    @Builder.Default
    private boolean stackable = false;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PromotionTarget> targets = new ArrayList<>();

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PromotionBundleItem> bundleItems = new ArrayList<>();

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PromotionRequirement> requirements = new ArrayList<>();

    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PromotionSchedule> schedules = new ArrayList<>();
}
