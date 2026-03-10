package com.veo.backend.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PrescriptionRequest {
    private String prescriptionImageUrl;
    private BigDecimal sphereOd;
    private BigDecimal sphereOs;
    private BigDecimal cylinderOd;
    private BigDecimal cylinderOs;
    private Integer axisOd;
    private Integer axisOs;
    private BigDecimal pd;
}
