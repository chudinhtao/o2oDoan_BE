package com.fnb.order.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class TableActionRequest {
    @NotNull(message = "ID bàn nguồn không được trống")
    private UUID sourceTableId;

    @NotNull(message = "ID bàn đích không được trống")
    private UUID targetTableId;
}
