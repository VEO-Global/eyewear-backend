package com.veo.backend.dto.response;

import com.veo.backend.enums.PaymentMethod;
import com.veo.backend.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentSummaryResponse {
    private Long paymentId;
    private PaymentMethod method;
    private PaymentStatus status;
    private BigDecimal amount;
    private String transactionCode;
    private String paymentProofImg;
    private LocalDateTime expiredAt;
    private LocalDateTime paidAt;
}
