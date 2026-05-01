package com.fnb.menu.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemResponse {
    private UUID id;
    private UUID categoryId;
    private String categoryName;
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private java.time.LocalDateTime saleStartAt;
    private java.time.LocalDateTime saleEndAt;
    private List<PromotionResponse.ScheduleResponse> schedules;
    private String station;
    
    @JsonProperty("isAvailable")
    private boolean isAvailable;
    
    @JsonProperty("isFeatured")
    private boolean isFeatured;
    
    @JsonProperty("isActive")
    private boolean isActive;
    
    private List<OptionGroupResponse> optionGroups;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionGroupResponse {
        private UUID id;
        private String name;
        private String type;
        
        @JsonProperty("isRequired")
        private boolean isRequired;
        
        private int displayOrder;
        private List<OptionResponse> options;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionResponse {
        private UUID id;
        private String name;
        private BigDecimal extraPrice;
        
        @JsonProperty("isAvailable")
        private boolean isAvailable;
    }
}
