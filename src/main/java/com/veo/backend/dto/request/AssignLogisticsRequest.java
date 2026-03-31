package com.veo.backend.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AssignLogisticsRequest {
    private String carrier;
    private String shippingMethod;
    private LocalDate estimatedDeliveryDate;
}
