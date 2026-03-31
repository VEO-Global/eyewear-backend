package com.veo.backend.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantResponse {
    private Long id;
    private Long productId;

    private String sku;

    private String color;

    private String size;

    private BigDecimal price;

    private Integer stockQuantity;

    private LocalDateTime expectedRestockDate;

    private boolean isActive;
}
