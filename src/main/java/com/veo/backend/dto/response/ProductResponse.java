package com.veo.backend.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    private Boolean isActive;

    private LocalDateTime createdAt;
}
