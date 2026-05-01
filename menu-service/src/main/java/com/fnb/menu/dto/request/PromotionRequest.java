package com.fnb.menu.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
public class PromotionRequest {

    /** Mã code — bắt buộc nếu triggerType = COUPON */
    private String code;

    @NotBlank(message = "Tên khuyến mãi không được trống")
    private String name;

    /** PRODUCT | ORDER | BUNDLE */
    @NotBlank
    @Pattern(regexp = "PRODUCT|ORDER|BUNDLE", message = "scope phải là PRODUCT, ORDER hoặc BUNDLE")
    private String scope;

    /** AUTO | COUPON */
    @NotBlank
    @Pattern(regexp = "AUTO|COUPON", message = "triggerType phải là AUTO hoặc COUPON")
    private String triggerType;

    /** PERCENT | FIX_AMOUNT | FIX_PRICE */
    @NotBlank
    @Pattern(regexp = "PERCENT|FIX_AMOUNT|FIX_PRICE", message = "discountType không hợp lệ")
    private String discountType;

    @DecimalMin("0")
    private BigDecimal discountValue;

    @DecimalMin("0")
    private BigDecimal maxDiscount;

    @Min(0)
    private Integer usageLimit;

    @Min(0)
    private int priority = 0;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private boolean stackable = false;

    @Valid
    private List<TargetRequest> targets;

    @Valid
    private List<BundleItemRequest> bundleItems;

    @Valid
    private RequirementRequest requirement;

    @Valid
    private List<ScheduleRequest> schedules;

    @Data
    public static class TargetRequest {
        @NotBlank
        @Pattern(regexp = "ITEM|CATEGORY|GLOBAL")
        private String targetType;
        private UUID targetId;
    }

    @Data
    public static class BundleItemRequest {
        @NotNull
        private UUID itemId;
        @Min(1)
        private int quantity = 1;
        @NotBlank
        @Pattern(regexp = "BUY|GET")
        private String role;
    }

    @Data
    public static class RequirementRequest {
        @DecimalMin("0")
        private BigDecimal minOrderAmount = BigDecimal.ZERO;
        @Min(0)
        private int minQuantity = 0;
        private String memberLevel;
    }

    @Data
    public static class ScheduleRequest {
        @Min(0) @Max(6)
        private int dayOfWeek;
        @NotNull
        private LocalTime startTime;
        @NotNull
        private LocalTime endTime;
    }
}
