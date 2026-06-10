package com.fnb.order.dto.redis;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@Data
public class CartItemDto {
    private String cartItemId; // ID định danh tạm thời trong giỏ hàng để có thể xóa/sửa
    private UUID menuItemId;
    
    @JsonProperty("name")
    @JsonAlias("itemName")
    private String itemName;
    
    @JsonProperty("basePrice")
    @JsonAlias("unitPrice")
    private BigDecimal unitPrice;
    
    @JsonProperty("taxRate")
    private BigDecimal taxRate;
    
    @JsonProperty("discountPrice")
    private BigDecimal discountPrice;

    @JsonProperty("saleEndAt")
    private LocalDateTime saleEndAt;
    
    private int quantity;
    private String note;
    private String station;
    
    @JsonProperty("imageUrl")
    private String imageUrl;

    private List<CartItemOptionDto> options = new ArrayList<>();

    private boolean hasFlashSale = false;

    // Helper method tính thành tiền của 1 dòng
    @JsonProperty("lineTotal")
    public BigDecimal calculateItemTotal() {
        // Dùng giá đã giảm nếu có, ngược lại dùng giá gốc
        BigDecimal base = discountPrice != null ? discountPrice : (unitPrice != null ? unitPrice : BigDecimal.ZERO);
        BigDecimal total = base;
        if (options != null) {
            for (CartItemOptionDto option : options) {
                if (option.getExtraPrice() != null) {
                    total = total.add(option.getExtraPrice());
                }
            }
        }
        return total.multiply(BigDecimal.valueOf(quantity));
    }
}
