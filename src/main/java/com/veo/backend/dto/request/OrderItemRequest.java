package com.veo.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderItemRequest {
    @NotNull(message = "Product variant is required")
    private Long productVariantId;

    private Long variantId;

    private Long lensProductId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    public Long resolveVariantId() {
        return productVariantId != null ? productVariantId : variantId;
    }
}
