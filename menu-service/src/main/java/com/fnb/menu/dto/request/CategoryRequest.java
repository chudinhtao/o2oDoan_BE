package com.fnb.menu.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {
    @NotBlank(message = "Tên danh mục không được trống")
    private String name;

    private String imageUrl;

    private int displayOrder = 0;
}
