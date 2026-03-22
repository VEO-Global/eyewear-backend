package com.veo.backend.dto.response;

import com.veo.backend.enums.PrescriptionReviewStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PrescriptionReviewResponse {
    private Long prescriptionId;
    private Long orderId;
    private String orderCode;
    private String customerName;
    private String customerEmail;
    private String receiverName;
    private String phoneNumber;
    private LensSummaryResponse lens;
    private PrescriptionResponse prescription;
    private PrescriptionReviewStatus reviewStatus;
    private String reviewNote;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
