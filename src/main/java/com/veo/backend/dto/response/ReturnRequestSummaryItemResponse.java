package com.veo.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReturnRequestSummaryItemResponse {
    private Long returnRequestItemId;
    private Long orderItemId;
    private Long productVariantId;
    private String productName;
    private String variantName;
    private Integer quantity;
}
