package com.veo.backend.dto.request;

import com.veo.backend.enums.PaymentMethod;
import lombok.Data;

@Data
public class PaymentRequest {
    private Long orderId;
    private PaymentMethod paymentMethod;
}
