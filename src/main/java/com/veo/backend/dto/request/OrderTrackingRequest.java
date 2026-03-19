package com.veo.backend.dto.request;

import lombok.Data;

@Data
public class OrderTrackingRequest {
    private String trackingNumber;
    private String provider;
}
