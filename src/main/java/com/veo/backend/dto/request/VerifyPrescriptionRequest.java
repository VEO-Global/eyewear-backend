package com.veo.backend.dto.request;

import lombok.Data;

@Data
public class VerifyPrescriptionRequest {
    private boolean approved;
    private String staffNote;
}
