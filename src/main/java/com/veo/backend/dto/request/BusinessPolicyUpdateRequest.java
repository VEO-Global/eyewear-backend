package com.veo.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BusinessPolicyUpdateRequest {
    private String title;

    @NotBlank(message = "Policy content is required")
    private String content;

    private String description;
}
