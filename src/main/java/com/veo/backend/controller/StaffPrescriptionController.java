package com.veo.backend.controller;

import com.veo.backend.dto.request.PrescriptionReviewRequest;
import com.veo.backend.dto.response.PrescriptionReviewResponse;
import com.veo.backend.service.PrescriptionReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/staff", "/staff"})
@RequiredArgsConstructor
public class StaffPrescriptionController {
    private final PrescriptionReviewService prescriptionReviewService;

    @GetMapping("/prescriptions/pending")
    public List<PrescriptionReviewResponse> getPendingPrescriptions() {
        return prescriptionReviewService.getPendingPrescriptions();
    }

    @GetMapping("/orders/{orderId}/prescription")
    public PrescriptionReviewResponse getOrderPrescription(@PathVariable Long orderId) {
        return prescriptionReviewService.getOrderPrescription(orderId);
    }

    @PatchMapping("/prescriptions/{id}/review")
    public PrescriptionReviewResponse reviewPrescription(
            @PathVariable Long id,
            @Valid @RequestBody PrescriptionReviewRequest request,
            Authentication authentication
    ) {
        return prescriptionReviewService.reviewPrescription(authentication.getName(), id, request);
    }
}
