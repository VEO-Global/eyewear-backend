package com.veo.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentConfirmRequest {
    @NotBlank(message = "Transaction code is required")
    private String transactionCode;

    private String paymentProofImg;
}
