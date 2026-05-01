package com.fnb.menu.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuUpdatedEvent {
    private UUID itemId;
    private UUID optionId;
    private String type; // "ITEM" or "OPTION"
    private boolean isAvailable;
    @Builder.Default
    private boolean isActive = true;
    private LocalDateTime updatedAt;
}
