package com.veo.backend.service.impl;

import com.veo.backend.dto.request.PaymentConfirmRequest;
import com.veo.backend.dto.request.PaymentRequest;
import com.veo.backend.dto.response.PaymentQrResponse;
import com.veo.backend.dto.response.PaymentSummaryResponse;
import com.veo.backend.entity.Order;
import com.veo.backend.entity.Payment;
import com.veo.backend.entity.User;
import com.veo.backend.enums.OrderStatus;
import com.veo.backend.enums.PaymentMethod;
import com.veo.backend.enums.PaymentStatus;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.OrderRepository;
import com.veo.backend.repository.PaymentRepository;
import com.veo.backend.repository.UserRepository;
import com.veo.backend.service.PaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PaymentQrResponse getPaymentQr(String email, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));

        String transferContent = "VEO" + order.getId();

        String qrUrl = String.format("https://img.vietqr.io/image/MB-123456789-qr_only.png?amount=%s&addInfo=%s",
                order.getTotalAmount().toBigInteger(), transferContent );

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(new Payment());
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setMethod(PaymentMethod.BANK_TRANSFER);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionCode(order.getOrderCode());
        payment.setExpiredAt(LocalDateTime.now().plusHours(24));
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return PaymentQrResponse.builder()
                .orderId(order.getId())
                .orderCode("ORD-" + order.getId())
                .paymentStatus(PaymentStatus.PENDING)
                .qrCodeUrl(qrUrl)
                .qrRawData("00020101021238580010A000000727...")
                .bankName("MB Bank")
                .bankAccountNumber("0987654321")
                .bankAccountName("CONG TY VEO EYEWEAR")
                .transferContent(transferContent)
                .amountToPay(order.getTotalAmount())
                .expiredAt(payment.getExpiredAt())
                .build();
    }

    @Override
    public PaymentSummaryResponse getPaymentStatus(String email, Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(()  -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));
        return mapToSummary(payment);
    }

    @Override
    @Transactional
    public PaymentSummaryResponse confirmPayment(String email, Long orderId, PaymentConfirmRequest request) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND, "Payment not found"));

        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionCode(request.getTransactionCode());
        payment.setPaymentProofImg(request.getPaymentProofImg());
        payment.setPaidAt(LocalDateTime.now());

        payment.getOrder().setStatus(OrderStatus.PENDING_PAYMENT);

        return mapToSummary(paymentRepository.save(payment));
    }

    @Override
    public List<PaymentSummaryResponse> getMyPayments(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        return paymentRepository.findByOrderUserId(user.getId()).stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentSummaryResponse> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByOrderUserId(userId).stream()
                .map(this::mapToSummary).collect(Collectors.toList());
    }

    @Override
    public List<PaymentSummaryResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable).stream()
                .map(this::mapToSummary).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String processPayment(PaymentRequest request, String userEmail) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));

        if (order.getUser() == null || !order.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "User cannot do this process");
        }

        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND, "Payment information not found"));

        if (payment.getStatus() ==  PaymentStatus.PAID) {
            return "Payment has been paid";
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setMethod(request.getPaymentMethod());
        paymentRepository.save(payment);

        order.setStatus(OrderStatus.PENDING_VERIFICATION);
        orderRepository.save(order);
        return "Payment successfully";
    }

    private PaymentSummaryResponse mapToSummary(Payment p) {
        return PaymentSummaryResponse.builder()
                .paymentId(p.getId())
                .method(p.getMethod())
                .status(p.getStatus())
                .amount(p.getAmount())
                .transactionCode(p.getTransactionCode())
                .paymentProofImg(p.getPaymentProofImg())
                .expiredAt(p.getExpiredAt())
                .paidAt(p.getPaidAt())
                .build();
    }
}
