package com.fnb.inventory.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class LocationResponse {
    private UUID id;
    private String name;
    private String description;
    private boolean isActive;
}
