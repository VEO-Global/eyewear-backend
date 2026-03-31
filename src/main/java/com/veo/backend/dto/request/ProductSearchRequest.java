package com.veo.backend.dto.request;

import com.veo.backend.enums.ProductCatalogType;
import com.veo.backend.enums.ProductStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductSearchRequest {
    private String keyword;
    private Long categoryId;
    private ProductStatus status;
    private ProductCatalogType catalogType;
    private Boolean active;
    private int page;
    private int size;
}
