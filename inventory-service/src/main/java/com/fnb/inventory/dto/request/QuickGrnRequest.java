package com.fnb.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Nhập nhanh (Quick GRN) - không cần PO.
 * Dành cho tình huống hàng về gấp lúc đông khách.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickGrnRequest {

    @NotNull(message = "Item ID là bắt buộc")
    private UUID itemId;

    private UUID locationId;

    @NotNull(message = "Số lượng nhập là bắt buộc")
    @Positive(message = "Số lượng phải là số dương")
    private BigDecimal quantity;

    /** Giá nhập tại thời điểm này (để tính Moving Average Cost). Có thể null nếu chưa biết giá. */
    private BigDecimal unitPrice;

    /** Ghi chú nhanh (tên nhà cung cấp, số lô hàng, v.v.) */
    private String note;

    private String lotNumber;
    private java.time.LocalDate expiryDate;
}
