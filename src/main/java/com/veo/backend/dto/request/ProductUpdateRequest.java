package com.veo.backend.dto.request;

import com.veo.backend.enums.ProductCatalogType;
import com.veo.backend.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequest {
    private String name;

    private String brand;

    private String description;

    private BigDecimal basePrice;

    private String material;

    private String gender;

    private String model3dUrl;

    private Boolean isActive;

    private Long categoryId;

    private ProductStatus status;

    private ProductCatalogType catalogType;
}
