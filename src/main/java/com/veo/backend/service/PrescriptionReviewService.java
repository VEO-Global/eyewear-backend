package com.veo.backend.service;

import com.veo.backend.dto.request.PrescriptionReviewRequest;
import com.veo.backend.dto.response.PrescriptionReviewResponse;

import java.util.List;

public interface PrescriptionReviewService {
    List<PrescriptionReviewResponse> getPendingPrescriptions();

    PrescriptionReviewResponse getOrderPrescription(Long orderId);

    PrescriptionReviewResponse reviewPrescription(String reviewerEmail, Long prescriptionId, PrescriptionReviewRequest request);
}
