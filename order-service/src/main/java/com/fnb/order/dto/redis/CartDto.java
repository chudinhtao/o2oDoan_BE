package com.fnb.order.dto.redis;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class CartDto {
    private String sessionToken;
    private List<CartItemDto> items = new ArrayList<>();
    
    // Thuộc tính tiện ích tính tổng tiền giỏ hàng (Không lưu xuống DB, tự động tính realtime)
    @com.fasterxml.jackson.annotation.JsonProperty("totalAmount")
    private BigDecimal cartTotal = BigDecimal.ZERO;
    
    @com.fasterxml.jackson.annotation.JsonProperty("originalTotal")
    private BigDecimal originalTotal = BigDecimal.ZERO; // Tổng trước giảm giá

    @com.fasterxml.jackson.annotation.JsonProperty("automatedDiscount")
    private BigDecimal automatedDiscount = BigDecimal.ZERO; // Tổng cộng giảm giá tự động (Combo + Flash Sale)

    @com.fasterxml.jackson.annotation.JsonProperty("appliedPromotions")
    private List<String> appliedPromotions = new ArrayList<>();

    public void recalculateTotal() {
        cartTotal = BigDecimal.ZERO;
        originalTotal = BigDecimal.ZERO;
        if (items != null) {
            for (CartItemDto item : items) {
                // Ta lấy giá gốc để tính Original
                BigDecimal baseUnit = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal opTot = BigDecimal.ZERO;
                if (item.getOptions() != null) {
                    for (CartItemOptionDto op : item.getOptions()) {
                        if (op.getExtraPrice() != null) opTot = opTot.add(op.getExtraPrice());
                    }
                }
                originalTotal = originalTotal.add(baseUnit.add(opTot).multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        
        // Giá cuối = Giá gốc - Giảm giá tự động
        cartTotal = originalTotal.subtract(automatedDiscount != null ? automatedDiscount : BigDecimal.ZERO).max(BigDecimal.ZERO);
    }
}
