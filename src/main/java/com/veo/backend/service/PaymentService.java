package com.veo.backend.service;

import com.veo.backend.dto.request.PaymentConfirmRequest;
import com.veo.backend.dto.request.PaymentRequest;
import com.veo.backend.dto.response.PaymentQrResponse;
import com.veo.backend.dto.response.PaymentSummaryResponse;

import org.springframework.data.domain.Pageable;
import java.util.List;

public interface PaymentService {
    PaymentQrResponse getPaymentQr(String email, Long orderId);

    PaymentSummaryResponse getPaymentStatus(String email, Long orderId);

    PaymentSummaryResponse confirmPayment(String email, Long orderId, PaymentConfirmRequest request);

    List<PaymentSummaryResponse> getMyPayments(String email);

    List<PaymentSummaryResponse> getPaymentsByUserId(Long userId);

    List<PaymentSummaryResponse> getAllPayments(Pageable pageable);

    String processPayment(PaymentRequest request, String userEmail);
}
