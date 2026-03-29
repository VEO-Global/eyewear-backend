package com.veo.backend.dto.request;

import com.veo.backend.enums.PaymentMethod;

public class PaymentRequest {
    private Long orderId;
    private PaymentMethod paymentMethod;
}
