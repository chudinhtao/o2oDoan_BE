package com.fnb.menu.controller;

import com.fnb.menu.dto.response.PromotionResponse;
import com.fnb.menu.service.PromotionService;
import com.fnb.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Public endpoints — dùng by OrderService (Pricing Engine) hoặc Customer App.
 */
@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    /**
     * Validate mã Coupon khi khách nhập vào giỏ hàng (Level 3 - Order Discount).
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<PromotionResponse>> validate(
            @RequestParam String code,
            @RequestParam(required = false) BigDecimal orderAmount) {
        return ResponseEntity.ok(ApiResponse.ok(promotionService.validateCoupon(code, orderAmount)));
    }

    /**
     * Lấy toàn bộ CTKM đang active — OrderService dùng cho Pricing Engine Level 1 & 3.
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> getActive() {
        return ResponseEntity.ok(ApiResponse.ok(promotionService.getActivePromotions()));
    }

    /**
     * Lấy CTKM active theo scope (PRODUCT / ORDER / BUNDLE).
     */
    @GetMapping("/active/{scope}")
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> getActiveByScope(
            @PathVariable String scope) {
        return ResponseEntity.ok(ApiResponse.ok(promotionService.getActiveByScope(scope)));
    }

    /** OrderService gọi sau khi Checkout thành công để tăng used_count của Voucher. */
    @PostMapping("/{id}/increment-usage")
    public ApiResponse<String> incrementUsage(@PathVariable UUID id) {
        promotionService.incrementUsedCount(id);
        return ApiResponse.ok("Incremented", null);
    }
}
