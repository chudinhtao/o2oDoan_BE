package com.fnb.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.UUID;

@Data
public class TicketItemOptionRequest {

    private UUID optionId;
}
