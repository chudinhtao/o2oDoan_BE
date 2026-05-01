package com.fnb.menu.dto.request;

import jakarta.validation.Valid;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class MenuItemRequest {
    @NotNull(message = "categoryId không được null")
    private UUID categoryId;

    @NotBlank(message = "Tên món không được trống")
    private String name;

    private String description;
    private String imageUrl;

    @NotNull(message = "Giá không được null")
    @DecimalMin(value = "0", message = "Giá phải >= 0")
    private BigDecimal basePrice;

    private BigDecimal salePrice;

    @NotBlank(message = "Station không được trống")
    @Pattern(regexp = "HOT|COLD|DRINK", message = "Station phải là HOT, COLD hoặc DRINK")
    private String station;

    @JsonProperty("isFeatured")
    private boolean isFeatured = false;

    @JsonProperty("isAvailable")
    private boolean isAvailable = true;

    @Valid
    private List<OptionGroupRequest> optionGroups;

    @Data
    public static class OptionGroupRequest {
        @NotBlank
        private String name;

        @Pattern(regexp = "SINGLE|MULTI")
        private String type;

        @JsonProperty("isRequired")
        private boolean isRequired = false;
        private int displayOrder = 0;

        @Valid
        private List<OptionRequest> options;
    }

    @Data
    public static class OptionRequest {
        @NotBlank
        private String name;

        @DecimalMin("0")
        private BigDecimal extraPrice = BigDecimal.ZERO;
    }
}
