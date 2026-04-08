package com.veo.backend.service;

import com.veo.backend.dto.request.PaymentConfirmRequest;
import com.veo.backend.dto.request.PayosPaymentActionRequest;
import com.veo.backend.dto.response.PagedResponse;
import com.veo.backend.dto.response.PaymentQrResponse;
import com.veo.backend.dto.response.PaymentSummaryResponse;
import com.veo.backend.dto.response.RevenuePointResponse;
import com.veo.backend.dto.response.RevenueSummaryResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.List;

public interface PaymentService {
    PaymentQrResponse getPaymentQr(String email, Long orderId);

    PaymentSummaryResponse getPaymentStatus(String email, Long orderId);

    PaymentSummaryResponse confirmPayment(String email, Long orderId, PaymentConfirmRequest request);

    PaymentSummaryResponse confirmFakePayosPayment(String email, PayosPaymentActionRequest request);

    PaymentSummaryResponse approveFakePayosPayment(String actorEmail, PayosPaymentActionRequest request);

    PaymentSummaryResponse rejectFakePayosPayment(String actorEmail, PayosPaymentActionRequest request);

    List<PaymentSummaryResponse> getPendingFakePayosPayments();

    SseEmitter subscribeCustomerPaymentEvents(String email);

    SseEmitter subscribeStaffPaymentEvents(String email);

    List<PaymentSummaryResponse> getMyPayments(String email);

    List<PaymentSummaryResponse> getPaymentsByUserId(Long userId);

    PagedResponse<PaymentSummaryResponse> getAllPayments(Pageable pageable);

    RevenueSummaryResponse getRevenueSummary(LocalDate from, LocalDate to);

    List<RevenuePointResponse> getRevenueDaily(int year, int month);

    List<RevenuePointResponse> getRevenueMonthly(int year);

    String createPayosPaymentLink(Long orderId);

    void handlePayosReturn(Long orderCode);
}
