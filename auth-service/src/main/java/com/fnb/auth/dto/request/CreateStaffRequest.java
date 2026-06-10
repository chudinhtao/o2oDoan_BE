package com.fnb.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateStaffRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    @Pattern(regexp = "CASHIER|KITCHEN|SERVER", message = "Role phải là CASHIER, KITCHEN hoặc SERVER")
    private String role;

    private String fullName;

    private String phone;
}
