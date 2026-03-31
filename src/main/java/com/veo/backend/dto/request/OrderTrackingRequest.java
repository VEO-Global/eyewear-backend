package com.veo.backend.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class OrderTrackingRequest {
    @NotBlank(message = "Tracking number is required")
    private String trackingNumber;
    @NotBlank(message = "Provider is required")
    private String provider;
}
