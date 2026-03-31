package com.veo.backend.service;

import com.veo.backend.dto.request.PaymentConfirmRequest;
import com.veo.backend.dto.response.PagedResponse;
import com.veo.backend.dto.response.PaymentQrResponse;
import com.veo.backend.dto.response.PaymentSummaryResponse;
import com.veo.backend.dto.response.RevenuePointResponse;
import com.veo.backend.dto.response.RevenueSummaryResponse;

import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;

public interface PaymentService {
    PaymentQrResponse getPaymentQr(String email, Long orderId);

    PaymentSummaryResponse getPaymentStatus(String email, Long orderId);

    PaymentSummaryResponse confirmPayment(String email, Long orderId, PaymentConfirmRequest request);

    List<PaymentSummaryResponse> getMyPayments(String email);

    List<PaymentSummaryResponse> getPaymentsByUserId(Long userId);

    PagedResponse<PaymentSummaryResponse> getAllPayments(Pageable pageable);

    RevenueSummaryResponse getRevenueSummary(LocalDate from, LocalDate to);

    List<RevenuePointResponse> getRevenueDaily(int year, int month);

    List<RevenuePointResponse> getRevenueMonthly(int year);

    String createPayosPaymentLink(Long orderId);

    void handlePayosReturn(Long orderCode);
}
