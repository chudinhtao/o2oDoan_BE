package com.fnb.inventory.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SupplierResponse {
    private UUID id;
    private String code;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String taxCode;
    private boolean isActive;
}
