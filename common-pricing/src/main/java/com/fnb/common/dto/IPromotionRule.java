package com.fnb.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface IPromotionRule {
    String getName();
    String getCode();
    Integer getPriority();
    String getDiscountType();
    BigDecimal getDiscountValue();
    BigDecimal getMaxDiscount();
    LocalDateTime getEndAt();
}
