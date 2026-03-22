package com.veo.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {
    private Long id;
    private Long orderItemId;
    private Long productVariantId;
    private Long productId;
    private String productName;
    private String productVariantName;
    private String variantName;
    private Long lensProductId;
    private String lensProductName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private BigDecimal price;
    private String thumbnailUrl;
}
