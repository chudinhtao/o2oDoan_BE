package com.fnb.inventory.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UomResponse {
    private UUID id;
    private String name;
    private String shortName;
}
