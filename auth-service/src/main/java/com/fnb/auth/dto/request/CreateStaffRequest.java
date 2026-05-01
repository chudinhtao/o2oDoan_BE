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
    @Pattern(regexp = "CASHIER|KITCHEN", message = "Role phải là CASHIER hoặc KITCHEN")
    private String role;

    private String fullName;
}
