package com.veo.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BulkVariantStockUpdateRequest {
    @NotEmpty(message = "Items are required")
    @jakarta.validation.Valid
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull(message = "Variant id is required")
        private Long variantId;

        @NotNull(message = "Stock quantity is required")
        @Min(value = 0, message = "Stock quantity must be at least 0")
        private Integer stockQuantity;
    }
}
