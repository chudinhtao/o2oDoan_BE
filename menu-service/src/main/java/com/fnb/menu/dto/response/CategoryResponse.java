package com.fnb.menu.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor // Bắt buộc cho Jackson deserialize
@AllArgsConstructor // Bắt buộc để @Builder hoạt động khi đã có @NoArgsConstructor
public class CategoryResponse {
    private UUID id;
    private String name;
    private String imageUrl;
    private int displayOrder;
    
    @JsonProperty("isActive")
    private boolean isActive;
}