package com.veo.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartItemResponse {
    private Long itemId;
    private Long productVariantId;
    private Long lensProductId;
    private Integer quantity;
    private String productName;
    private String variantSku;
    private String color;
    private String size;
    private String lensName;
}
