package com.veo.backend.service.impl;

import com.veo.backend.dto.request.PaymentConfirmRequest;
import com.veo.backend.dto.response.PagedResponse;
import com.veo.backend.dto.response.PaymentQrResponse;
import com.veo.backend.dto.response.PaymentSummaryResponse;
import com.veo.backend.dto.response.RevenuePointResponse;
import com.veo.backend.dto.response.RevenueSummaryResponse;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
        Order order = resolveOwnedOrder(email, orderId);

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
        payment.setPaidAt(null);
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
        resolveOwnedOrder(email, orderId);
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

        if (payment.getOrder() != null && payment.getOrder().getStatus() == OrderStatus.PENDING_PAYMENT) {
            payment.getOrder().setStatus(OrderStatus.PENDING_VERIFICATION);
            payment.getOrder().setUpdatedAt(LocalDateTime.now());
        }

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
    public PagedResponse<PaymentSummaryResponse> getAllPayments(Pageable pageable) {
        var page = paymentRepository.findAll(pageable);
        return PagedResponse.<PaymentSummaryResponse>builder()
                .content(page.getContent().stream().map(this::mapToSummary).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    public RevenueSummaryResponse getRevenueSummary(LocalDate from, LocalDate to) {
        LocalDateTime start = atStartOfDayOrMin(from);
        LocalDateTime end = atEndOfDayOrNow(to);

        List<Payment> payments = paymentRepository.findByStatus(PaymentStatus.PAID).stream()
                .filter(payment -> payment.getPaidAt() != null)
                .filter(payment -> !payment.getPaidAt().isBefore(start) && !payment.getPaidAt().isAfter(end))
                .toList();

        BigDecimal totalRevenue = sumAmounts(payments);
        BigDecimal averageOrderValue = payments.isEmpty()
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(payments.size()), 2, RoundingMode.HALF_UP);

        return RevenueSummaryResponse.builder()
                .fromDate(start.toLocalDate().toString())
                .toDate(end.toLocalDate().toString())
                .totalPaidOrders(payments.size())
                .totalRevenue(totalRevenue)
                .averageOrderValue(averageOrderValue)
                .build();
    }

    @Override
    public List<RevenuePointResponse> getRevenueDaily(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        List<Payment> paidPayments = paymentRepository.findByStatus(PaymentStatus.PAID);

        return yearMonth.atDay(1).datesUntil(yearMonth.plusMonths(1).atDay(1))
                .map(date -> {
                    List<Payment> payments = paidPayments.stream()
                            .filter(payment -> payment.getPaidAt() != null)
                            .filter(payment -> payment.getPaidAt().toLocalDate().equals(date))
                            .toList();
                    return RevenuePointResponse.builder()
                            .label(date.toString())
                            .totalOrders(payments.size())
                            .totalRevenue(sumAmounts(payments))
                            .build();
                })
                .toList();
    }

    @Override
    public List<RevenuePointResponse> getRevenueMonthly(int year) {
        List<Payment> paidPayments = paymentRepository.findByStatus(PaymentStatus.PAID);

        return java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(month -> {
                    List<Payment> payments = paidPayments.stream()
                            .filter(payment -> payment.getPaidAt() != null)
                            .filter(payment -> payment.getPaidAt().getYear() == year && payment.getPaidAt().getMonthValue() == month)
                            .toList();
                    return RevenuePointResponse.builder()
                            .label(String.format("%04d-%02d", year, month))
                            .totalOrders(payments.size())
                            .totalRevenue(sumAmounts(payments))
                            .build();
                })
                .toList();
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
                .orderId(p.getOrder() != null ? p.getOrder().getId() : null)
                .orderCode(p.getOrder() != null ? p.getOrder().getOrderCode() : null)
                .customerId(p.getOrder() != null && p.getOrder().getUser() != null ? p.getOrder().getUser().getId() : null)
                .customerName(p.getOrder() != null && p.getOrder().getUser() != null ? p.getOrder().getUser().getFullName() : null)
                .customerEmail(p.getOrder() != null && p.getOrder().getUser() != null ? p.getOrder().getUser().getEmail() : null)
                .method(p.getMethod())
                .status(p.getStatus())
                .amount(p.getAmount())
                .transactionCode(p.getTransactionCode())
                .paymentProofImg(p.getPaymentProofImg())
                .createdAt(p.getCreatedAt())
                .expiredAt(p.getExpiredAt())
                .paidAt(p.getPaidAt())
                .build();
    }

    private Order resolveOwnedOrder(String email, Long orderId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));

        if (order.getUser() == null || !user.getId().equals(order.getUser().getId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "You do not have permission to access this payment");
        }

        return order;
    }

    private BigDecimal sumAmounts(List<Payment> payments) {
        return payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDateTime atStartOfDayOrMin(LocalDate date) {
        return (date == null ? LocalDate.now().minusDays(29) : date).atStartOfDay();
    }

    private LocalDateTime atEndOfDayOrNow(LocalDate date) {
        LocalDate localDate = date == null ? LocalDate.now() : date;
        return localDate.atTime(23, 59, 59);
    }
}
