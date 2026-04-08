package com.veo.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PayosPaymentActionRequest {
    @NotNull(message = "Order id is required")
    private Long orderId;
}
