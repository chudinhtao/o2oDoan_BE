package com.fnb.common.dto;

import java.math.BigDecimal;

public interface IPromotionRule {
    String getName();
    String getCode();
    Integer getPriority();
    String getDiscountType();
    BigDecimal getDiscountValue();
    BigDecimal getMaxDiscount();
    java.time.LocalDateTime getEndAt();
}
