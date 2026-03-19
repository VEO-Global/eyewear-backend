package com.veo.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {
    private Long id;
    private Long productVariantId;
    private String productVariantName;
    private Long lensProductId;
    private String lensProductName;
    private Integer quantity;
    private BigDecimal price;
}
