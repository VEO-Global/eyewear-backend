package com.veo.backend.dto.response;

import com.veo.backend.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentRealtimeEventResponse {
    private String eventType;
    private Long orderId;
    private String orderCode;
    private PaymentStatus paymentStatus;
    private String message;
    private String redirectUrl;
    private PaymentSummaryResponse payment;
}
