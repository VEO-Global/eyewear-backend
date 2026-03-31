package com.veo.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProductBundleResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal bundlePrice;
    private Boolean isActive;
    private List<Long> productVariantIds;
    private List<Long> lensProductIds;
    private List<ProductVariantResponse> productVariants;
    private List<LensProductResponse> lensProducts;
}
