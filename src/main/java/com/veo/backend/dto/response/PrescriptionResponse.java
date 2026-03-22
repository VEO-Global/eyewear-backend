package com.veo.backend.dto.response;

import com.veo.backend.enums.PrescriptionReviewStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PrescriptionResponse {
    private String prescriptionImageUrl;
    private BigDecimal sphereOd;
    private BigDecimal sphereOs;
    private BigDecimal cylinderOd;
    private BigDecimal cylinderOs;
    private Integer axisOd;
    private Integer axisOs;
    private BigDecimal pd;
    private PrescriptionReviewStatus reviewStatus;
    private String reviewNote;
}
