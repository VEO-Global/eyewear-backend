package com.veo.backend.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductBundleRequest {
    private String name;
    private String description;
    private BigDecimal bundlePrice;
    private List<Long> productVariantIds;
    private List<Long> lensProductIds;
    private Boolean isActive;
}