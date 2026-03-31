package com.veo.backend.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class SetTrackingRequest {
    @NotBlank(message = "Tracking number is required")
    private String trackingNumber;
}
