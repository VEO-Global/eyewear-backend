package com.veo.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LensProductResponse {
    private Long id;
    private String name;
    private String type;
    private BigDecimal refractionIndex;
    private String description;
    private BigDecimal price;
}
