package com.veo.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderCreateResponse {
    private Long orderId;
    private BigDecimal totalAmount;
    private String message;
}
