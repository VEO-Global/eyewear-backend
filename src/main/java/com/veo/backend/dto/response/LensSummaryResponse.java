package com.veo.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LensSummaryResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private String description;
}
