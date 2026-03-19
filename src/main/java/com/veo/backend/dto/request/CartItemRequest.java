package com.veo.backend.dto.request;

import lombok.Data;

@Data
public class CartItemRequest {
    private Long productVariantId;
    private Long lensProductId;
    private Integer quantity;
}
