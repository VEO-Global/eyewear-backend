package com.veo.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantUpdateRequest {
    private String color;

    private String size;

    private BigDecimal price;

    private Integer stockQuantity;

    private LocalDateTime expectedRestockDate;

    private Boolean isActive;
}
