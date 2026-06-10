package com.fnb.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptRequest {

    @NotEmpty(message = "Phải có ít nhất 1 dòng nhận hàng")
    @Valid
    private List<ReceiptLineRequest> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiptLineRequest {

        @NotNull(message = "PO Item ID không được để trống")
        private UUID poItemId;

        private UUID locationId;

        @NotNull(message = "Số lượng thực nhận không được để trống")
        @DecimalMin(value = "0.0001", message = "Số lượng thực nhận phải lớn hơn 0")
        private BigDecimal receivedQuantity;

        /** Ghi chú cho lần nhận này (tùy chọn) */
        private String note;
    }
}
