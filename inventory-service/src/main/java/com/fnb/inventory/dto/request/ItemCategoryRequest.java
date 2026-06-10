package com.fnb.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ItemCategoryRequest {
    @NotBlank(message = "Tên nhóm nguyên liệu không được để trống")
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String description;
}
