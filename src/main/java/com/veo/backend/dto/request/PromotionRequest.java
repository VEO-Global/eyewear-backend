package com.veo.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PromotionRequest {
    @NotBlank(message = "Promotion code is required")
    private String code;

    @NotNull(message = "Discount percent is required")
    @Min(value = 1, message = "Discount percent must be at least 1")
    @Max(value = 100, message = "Discount percent must be at most 100")
    private Integer discountPercent;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal maxDiscountAmount;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be non-negative")
    private Integer quantity;

    private Boolean isActive;
}
