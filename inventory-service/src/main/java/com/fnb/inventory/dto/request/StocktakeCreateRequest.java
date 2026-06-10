package com.fnb.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StocktakeCreateRequest {
    
    @NotBlank(message = "Tên kỳ kiểm kê không được để trống")
    @Size(max = 255)
    private String name;

    private java.util.UUID locationId;

    @Size(max = 500)
    private String notes;
}
