package com.veo.backend.dto.request;

import lombok.Data;

@Data
public class AssignLogisticsRequest {
    private String carrier;
    private String shippingMethod;
    private String estimatedDeliveryDate;
}
