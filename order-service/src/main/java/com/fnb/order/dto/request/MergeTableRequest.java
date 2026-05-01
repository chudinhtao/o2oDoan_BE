package com.fnb.order.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class MergeTableRequest {
    @NotEmpty(message = "Danh sách bàn gộp không được trống")
    private List<UUID> sourceTableIds;

    @NotNull(message = "ID bàn đích không được trống")
    private UUID targetTableId;
}
