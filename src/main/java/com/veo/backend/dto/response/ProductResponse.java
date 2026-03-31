package com.veo.backend.dto.response;

import com.veo.backend.enums.ProductCatalogType;
import com.veo.backend.enums.ProductStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;

    private String name;

    private String brand;

    private String description;

    private BigDecimal basePrice;

    private String material;

    private String gender;

    private String model3dUrl;

    private String imageUrl;

    private String image;

    private List<String> imageUrls;

    private ProductStatus status;

    private ProductCatalogType catalogType;

    private Integer stockQuantity;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private Long  categoryId;

    private List<ProductImageResponse> images;

    private List<ProductVariantResponse> variants;
}
