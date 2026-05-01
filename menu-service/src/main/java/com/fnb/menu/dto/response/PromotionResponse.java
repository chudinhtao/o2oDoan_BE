package com.fnb.menu.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PromotionResponse {

    private UUID id;
    private String code;
    private String name;
    private String scope;
    private String triggerType;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscount;
    private Integer usageLimit;
    private int usedCount;
    private int priority;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private boolean stackable;
    private boolean active;
    private LocalDateTime createdAt;
    private String displayStatus; // ACTIVE, SCHEDULED, EXPIRED, DISABLED

    private List<TargetResponse> targets;
    private List<BundleItemResponse> bundleItems;
    private RequirementResponse requirement;
    private List<ScheduleResponse> schedules;

    @Data
    @Builder
    public static class TargetResponse {
        private UUID id;
        private String targetType;
        private UUID targetId;
        private String targetName; // Bổ sung tên để AI/App dễ đọc
    }

    @Data
    @Builder
    public static class BundleItemResponse {
        private UUID id;
        private UUID itemId;
        private String itemName; // Bổ sung tên món
        private int quantity;
        private String role;
    }

    @Data
    @Builder
    public static class RequirementResponse {
        private UUID id;
        private BigDecimal minOrderAmount;
        private int minQuantity;
        private String memberLevel;
    }

    @Data
    @Builder
    public static class ScheduleResponse {
        private UUID id;
        private int dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;
    }
}
