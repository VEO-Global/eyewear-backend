package com.veo.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PromotionValidationRequest {
    @NotBlank(message = "Promotion code is required")
    private String code;
}
