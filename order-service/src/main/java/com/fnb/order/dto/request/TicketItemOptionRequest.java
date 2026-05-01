package com.fnb.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketItemOptionRequest {

    private java.util.UUID optionId;
}
