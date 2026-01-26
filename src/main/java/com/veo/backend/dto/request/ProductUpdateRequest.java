package com.veo.backend.dto.request;

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

    private Boolean isActive;

    private Long categoryId;
}
