package com.fnb.inventory.dto.request;

import com.fnb.inventory.enums.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class InventoryItemRequest {
    @Size(max = 50)
    private String sku;

    @NotBlank(message = "Tên nguyên liệu không được để trống")
    @Size(max = 255)
    private String name;

    private UUID categoryId;

    @NotNull(message = "Loại nguyên liệu không được để trống")
    private ItemType type; // RAW, RETAIL, CONSUMABLE

    @NotNull(message = "Đơn vị tính cơ sở không được để trống")
    private UUID baseUomId;

    private BigDecimal safetyStock;
    private BigDecimal avgCostPrice;
}
