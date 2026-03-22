package com.veo.backend.service;

import com.veo.backend.dto.request.PaymentConfirmRequest;
import com.veo.backend.dto.response.PaymentQrResponse;
import com.veo.backend.dto.response.PaymentSummaryResponse;

public interface PaymentService {
    PaymentQrResponse getPaymentQr(String email, Long orderId);

    PaymentSummaryResponse getPaymentStatus(String email, Long orderId);

    PaymentSummaryResponse confirmPayment(String email, Long orderId, PaymentConfirmRequest request);
}
