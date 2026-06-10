package com.fnb.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class UomConversionRequest {
    @NotNull(message = "Item ID không được để trống")
    private UUID itemId;

    @NotNull(message = "Đơn vị nguồn không được để trống")
    private UUID fromUomId;

    @NotNull(message = "Đơn vị đích không được để trống")
    private UUID toUomId;

    @NotNull(message = "Tỷ lệ quy đổi không được để trống")
    private BigDecimal conversionRate;
}
