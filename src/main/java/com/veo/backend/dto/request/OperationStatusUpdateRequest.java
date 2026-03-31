package com.veo.backend.dto.request;

import com.veo.backend.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OperationStatusUpdateRequest {
    @NotNull(message = "Status is required")
    private OrderStatus status;

    private String note;
}
