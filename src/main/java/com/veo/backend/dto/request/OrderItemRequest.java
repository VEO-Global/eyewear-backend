package com.veo.backend.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemRequest {
    private Long productVariantId;
    private Long lensProductId;
    private Integer quantity;
}
