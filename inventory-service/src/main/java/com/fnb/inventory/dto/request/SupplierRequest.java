package com.fnb.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupplierRequest {
    @Size(max = 50)
    private String code;

    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    @Size(max = 255)
    private String name;

    @Size(max = 20)
    private String phone;

    @Size(max = 100)
    private String email;

    @Size(max = 500)
    private String address;

    @Size(max = 50)
    private String taxCode;
}
