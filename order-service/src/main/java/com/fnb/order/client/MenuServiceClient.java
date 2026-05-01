package com.fnb.order.client;

import com.fnb.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@FeignClient(name = "menu-service")
public interface MenuServiceClient {

    record OptionDetail(
            UUID id,
            String name,
            BigDecimal extraPrice,
            Boolean isAvailable
    ) {}

    record OptionGroupDetail(
            UUID id,
            String name,
            List<OptionDetail> options
    ) {}

    record MenuItemDetail(
            UUID id,
            UUID categoryId,
            String name,
            BigDecimal basePrice,
            String station,
            String imageUrl,
            Boolean isAvailable,
            Boolean isActive,
            List<OptionGroupDetail> optionGroups
    ) {}

    /**
     * PromotionDetail chuẩn mới — mapping với PromotionResponse từ menu-service.
     * Dùng cho Pricing Engine (validate coupon + lấy danh sách active).
     */
    record PromotionDetail(
            UUID id,
            String code,
            String name,
            String scope,           // PRODUCT | ORDER | BUNDLE
            String triggerType,     // AUTO | COUPON
            String discountType,    // PERCENT | FIX_AMOUNT | FIX_PRICE
            BigDecimal discountValue,
            BigDecimal maxDiscount,
            Integer usageLimit,
            int usedCount,
            int priority,
            Boolean stackable,
            Boolean active,
            java.time.LocalDateTime startAt,
            java.time.LocalDateTime endAt,
            List<TargetDetail> targets,
            List<BundleItemDetail> bundleItems,
            RequirementDetail requirement,
            List<ScheduleDetail> schedules
    ) implements com.fnb.common.dto.IBundleRule {
        @Override public String getName() { return name; }
        @Override public String getCode() { return code; }
        @Override public Integer getPriority() { return priority; }
        @Override public String getDiscountType() { return discountType; }
        @Override public BigDecimal getDiscountValue() { return discountValue; }
        @Override public BigDecimal getMaxDiscount() { return maxDiscount; }
        @Override public List<BundleItemDetail> getBundleItems() { return bundleItems; }
        @Override public java.time.LocalDateTime getEndAt() { return endAt; }
    }

    record TargetDetail(UUID id, String targetType, UUID targetId) implements com.fnb.common.dto.ITarget {
        @Override public String getTargetType() { return targetType; }
        @Override public UUID getTargetId() { return targetId; }
    }

    record BundleItemDetail(UUID id, UUID itemId, int quantity, String role) implements com.fnb.common.dto.IBundleItem {
        @Override public UUID getItemId() { return itemId; }
        @Override public int getQuantity() { return quantity; }
        @Override public String getRole() { return role; }
    }

    record RequirementDetail(
            UUID id,
            BigDecimal minOrderAmount,
            int minQuantity,
            String memberLevel
    ) {}

    record ScheduleDetail(UUID id, int dayOfWeek, String startTime, String endTime) {}

    @GetMapping("/api/menu/items/{id}")
    ApiResponse<MenuItemDetail> getMenuItemById(@PathVariable("id") UUID id);

    /** Validate mã Coupon — OrderService gọi khi khách nhập mã */
    @GetMapping("/api/promotions/validate")
    ApiResponse<PromotionDetail> validatePromotion(
            @RequestParam("code") String code,
            @RequestParam("orderAmount") BigDecimal orderAmount
    );

    /** Lấy toàn bộ CTKM đang active — Pricing Engine Level 1 & Level 3 cần */
    @GetMapping("/api/promotions/active")
    ApiResponse<List<PromotionDetail>> getActivePromotions();

    /** Lấy active CTKM theo scope (PRODUCT / ORDER / BUNDLE) */
    @GetMapping("/api/promotions/active/{scope}")
    ApiResponse<List<PromotionDetail>> getActiveByScope(@PathVariable("scope") String scope);

    @PostMapping("/api/promotions/{id}/increment-usage")
    ApiResponse<String> incrementPromotionUsage(@PathVariable("id") UUID id);

    @PutMapping("/api/menu/internal/featured")
    ApiResponse<Void> updateFeaturedItems(@RequestBody List<UUID> itemIds);
}
