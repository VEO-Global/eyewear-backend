package com.veo.backend.dto.request;

import com.veo.backend.enums.PrescriptionReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PrescriptionReviewRequest {
    @NotNull(message = "Review status is required")
    private PrescriptionReviewStatus reviewStatus;

    private String reviewNote;
}
