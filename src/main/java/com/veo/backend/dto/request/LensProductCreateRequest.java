package com.veo.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LensProductCreateRequest {
    @NotBlank(message = "Lens name is required")
    private String name;

    @NotBlank(message = "Lens type is required")
    private String type;

    @NotNull(message = "Refraction index is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal refractionIndex;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    private Boolean isActive;
}
