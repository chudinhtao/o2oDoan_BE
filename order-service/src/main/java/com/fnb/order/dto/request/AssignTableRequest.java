package com.fnb.order.dto.request;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class AssignTableRequest {
    private List<UUID> tableIds;
}
