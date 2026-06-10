package com.fnb.inventory.dto.request;

import jakarta.validation.Valid;
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
public class InternalTransferRequest {

    @NotNull(message = "Kho xuất không được để trống")
    private UUID fromLocationId;

    @NotNull(message = "Kho nhập không được để trống")
    private UUID toLocationId;

    private String notes;

    @NotEmpty(message = "Danh sách mặt hàng luân chuyển không được rỗng")
    @Valid
    private List<TransferItemRequest> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferItemRequest {
        @NotNull(message = "Mặt hàng không được để trống")
        private UUID itemId;

        @NotNull(message = "Số lượng không được để trống")
        private BigDecimal quantity;

        private String lotNumber;
    }
}
