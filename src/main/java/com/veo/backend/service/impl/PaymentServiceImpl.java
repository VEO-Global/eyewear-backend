package com.veo.backend.service.impl;

import com.veo.backend.dto.request.PaymentConfirmRequest;
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
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PayOS payOS;

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
    public String createPayosPaymentLink(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));

        // Chống trùng: nếu đã PAID rồi thì không tạo link nữa
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(orderId);
        if (existingPayment.isPresent() && existingPayment.get().getStatus() == PaymentStatus.PAID) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_CONFIRMED, "Payment already completed");
        }

        BigDecimal total = order.getTotalAmount()
                .add(order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO)
                .subtract(order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO);
        int amount = total.intValue();

        String description = "VEO Don " + order.getId();
        String returnUrl = "http://localhost:8080/api/payments/payos/return";
        String cancelUrl = "http://localhost:8080/api/payments/payos/cancel";

        try {
            CreatePaymentLinkRequest paymentRequest = CreatePaymentLinkRequest.builder()
                    .orderCode(order.getId())
                    .amount(Long.valueOf(amount))
                    .description(description)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .build();

            CreatePaymentLinkResponse result = payOS.paymentRequests().create(paymentRequest);

            // Lưu Payment record với status PENDING
            Payment payment = existingPayment.orElse(new Payment());
            payment.setOrder(order);
            payment.setAmount(total);
            payment.setMethod(PaymentMethod.PAYOS);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setTransactionCode(String.valueOf(order.getId()));
            payment.setExpiredAt(LocalDateTime.now().plusHours(24));
            paymentRepository.save(payment);

            return result.getCheckoutUrl();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo link thanh toán PayOS: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void handlePayosReturn(Long orderCode) {
        Order order = orderRepository.findById(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));

        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElse(new Payment());

        // Idempotent: nếu đã PAID rồi thì bỏ qua
        if (payment.getStatus() == PaymentStatus.PAID) {
            return;
        }

        // Đánh dấu thanh toán thành công (FAKE - không verify thật)
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setMethod(PaymentMethod.PAYOS);
        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionCode(String.valueOf(orderCode));
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Chuyển order sang trạng thái tiếp theo
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.PENDING_VERIFICATION);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        }
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
