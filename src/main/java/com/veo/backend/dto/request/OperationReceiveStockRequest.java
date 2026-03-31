package com.veo.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OperationReceiveStockRequest {
    @NotEmpty(message = "At least one stock item is required")
    private List<@Valid Item> items;

    private String note;

    @Data
    public static class Item {
        @NotNull(message = "Variant id is required")
        private Long variantId;

        @NotNull(message = "Received quantity is required")
        @Min(value = 1, message = "Received quantity must be greater than 0")
        private Integer receivedQuantity;
    }
}
