package com.fnb.common.util;

import com.fnb.common.dto.IPromotionRule;
import com.fnb.common.dto.ITarget;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

public class PricingEngine {

    /**
     * Checks if a promotion is applicable to a specific item based on its targets.
     * Cấp độ ưu tiên: ITEM > CATEGORY > GLOBAL
     */
    public static boolean isApplicable(List<? extends ITarget> targets, UUID itemId, UUID categoryId) {
        if (targets == null || targets.isEmpty()) return false;
        
        return targets.stream().anyMatch(t -> 
            "GLOBAL".equals(t.getTargetType()) ||
            ("CATEGORY".equals(t.getTargetType()) && t.getTargetId() != null && t.getTargetId().equals(categoryId)) ||
            ("ITEM".equals(t.getTargetType()) && t.getTargetId() != null && t.getTargetId().equals(itemId))
        );
    }

    /**
     * Calculates the raw discount value based solely on the discount rule parameters.
     */
    public static BigDecimal calculateRawDiscount(BigDecimal basePrice, String type, BigDecimal value, BigDecimal maxCap) {
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0 || value == null || type == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;
        switch (type) {
            case "PERCENT":
                discount = basePrice.multiply(value).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                if (maxCap != null && maxCap.compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(maxCap) > 0) {
                    discount = maxCap;
                }
                break;
            case "FIX_AMOUNT":
            case "AMOUNT":
                discount = value;
                break;
            case "FIX_PRICE":
                discount = basePrice.subtract(value).max(BigDecimal.ZERO);
                break;
        }
        return discount;
    }

    /**
     * Identifies the best promotion from a list of valid active promotions for a single item/order.
     * Rule: Highest priority wins. If tied, max final discount wins.
     * Returns a wrapper with the selected promotion and final safe discount bounded by basePrice.
     */
    public static <T extends IPromotionRule> BestPromotionResult<T> selectBestPromotion(List<T> promos, BigDecimal basePrice) {
        if (promos == null || promos.isEmpty()) {
            return new BestPromotionResult<>(null, BigDecimal.ZERO);
        }

        BigDecimal safeBase = basePrice != null ? basePrice : BigDecimal.ZERO;
        T bestPromo = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;

        for (T p : promos) {
            BigDecimal rawDiscount = calculateRawDiscount(safeBase, p.getDiscountType(), p.getDiscountValue(), p.getMaxDiscount());
            BigDecimal finalDiscount = rawDiscount.min(safeBase); // Khong giam qua gia goc

            int thisPriority = p.getPriority() != null ? p.getPriority() : 0;
            int bestPriority = (bestPromo != null && bestPromo.getPriority() != null) ? bestPromo.getPriority() : 0;

            if (bestPromo == null ||
                thisPriority > bestPriority ||
                (thisPriority == bestPriority && finalDiscount.compareTo(bestDiscount) > 0)) {
                bestPromo = p;
                bestDiscount = finalDiscount;
            }
        }

        return new BestPromotionResult<>(bestPromo, bestDiscount);
    }

    public static class BestPromotionResult<T extends IPromotionRule> {
        private final T promotion;
        private final BigDecimal discountAmount;

        public BestPromotionResult(T promotion, BigDecimal discountAmount) {
            this.promotion = promotion;
            this.discountAmount = discountAmount;
        }

        public T getPromotion() { return promotion; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
    }
}
