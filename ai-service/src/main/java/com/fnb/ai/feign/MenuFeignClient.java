package com.fnb.ai.feign;

import com.fnb.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "menu-service")
public interface MenuFeignClient {

    @PostMapping("/api/menu/internal/items/bulk")
    ApiResponse<List<MenuItemInternalResponse>> getBulkItems(@RequestBody List<UUID> itemIds);

    @GetMapping("/api/promotions/active")
    ApiResponse<List<PromotionResponse>> getActivePromotions();

    // [Phase 1.5] Tong quan trang thai menu cho Admin AI
    @GetMapping("/api/menu/internal/overview")
    ApiResponse<MenuOverviewResponse> getMenuOverview();

    record MenuItemInternalResponse(
            UUID id,
            String name,
            String description,
            BigDecimal basePrice,
            BigDecimal salePrice,
            String categoryName,
            boolean isAvailable,
            String station
    ) {}

    record PromotionResponse(
            UUID id,
            String code,
            String name,
            String scope,
            String triggerType,
            String discountType,
            BigDecimal discountValue,
            BigDecimal maxDiscount,
            java.time.LocalDateTime startAt,
            java.time.LocalDateTime endAt,
            boolean stackable,
            boolean active,
            String displayStatus,
            RequirementResponse requirement,
            List<TargetResponse> targets,
            List<BundleItemResponse> bundleItems,
            List<ScheduleResponse> schedules
    ) {}

    record RequirementResponse(
            UUID id,
            BigDecimal minOrderAmount,
            int minQuantity,
            String memberLevel
    ) {}

    record TargetResponse(
            UUID id,
            String targetType,
            UUID targetId,
            String targetName
    ) {}

    record BundleItemResponse(
            UUID id,
            UUID itemId,
            String itemName,
            int quantity,
            String role
    ) {}

    record ScheduleResponse(
            UUID id,
            int dayOfWeek,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime
    ) {}

    // [Phase 1.5] Menu overview records
    record MenuOverviewResponse(
            long totalActiveItems,
            long unavailableItems,
            long itemsOnSale,
            long featuredItems,
            Map<String, Long> unavailableByStation,
            List<UnavailableItem> unavailableItemList
    ) {}

    record UnavailableItem(
            String name,
            String station,
            BigDecimal basePrice
    ) {}
}
