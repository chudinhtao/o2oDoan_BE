package com.fnb.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UomRequest {
    @NotBlank(message = "Tên đơn vị tính không được để trống")
    @Size(max = 50)
    private String name;

    @Size(max = 20)
    private String shortName;
}
