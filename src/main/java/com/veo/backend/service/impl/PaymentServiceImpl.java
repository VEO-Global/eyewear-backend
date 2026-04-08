package com.veo.backend.service.impl;

import com.veo.backend.dto.request.PaymentConfirmRequest;
import com.veo.backend.dto.request.PayosPaymentActionRequest;
import com.veo.backend.dto.response.PagedResponse;
import com.veo.backend.dto.response.PaymentQrResponse;
import com.veo.backend.dto.response.PaymentRealtimeEventResponse;
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
import com.veo.backend.service.PaymentRealtimeService;
import com.veo.backend.service.PaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.payos.PayOS;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Base64;
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
    private final PaymentRealtimeService paymentRealtimeService;

    @Override
    @Transactional
    public PaymentQrResponse getPaymentQr(String email, Long orderId) {
        Order order = resolveOwnedOrder(email, orderId);
        BigDecimal finalAmount = calculateFinalAmount(order);

        String transferContent = buildTransferContent(order);
        String qrRawData = buildFakeQrPayload(order, finalAmount, transferContent);
        String qrUrl = buildFakeQrDataUrl(order, finalAmount, transferContent);

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(new Payment());
        payment.setOrder(order);
        payment.setAmount(finalAmount);
        payment.setMethod(resolveQrPaymentMethod(payment));
        payment.setStatus(resolveQrPaymentStatus(payment));
        payment.setTransactionCode(defaultTransactionCode(order));
        payment.setExpiredAt(resolveExpiration(payment));
        if (payment.getStatus() == PaymentStatus.PENDING || payment.getStatus() == PaymentStatus.UNPAID) {
            payment.setPaidAt(null);
        }
        payment = paymentRepository.save(payment);

        return PaymentQrResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .paymentStatus(payment.getStatus())
                .qrCodeUrl(qrUrl)
                .qrRawData(qrRawData)
                .bankName("MB Bank")
                .bankAccountNumber("0987654321")
                .bankAccountName("CONG TY VEO EYEWEAR")
                .transferContent(transferContent)
                .amountToPay(finalAmount)
                .expiredAt(payment.getExpiredAt())
                .build();
    }

    @Override
    public PaymentSummaryResponse getPaymentStatus(String email, Long orderId) {
        resolveOwnedOrder(email, orderId);
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));
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
        payment.setAmount(calculateFinalAmount(payment.getOrder()));
        payment.setPaidAt(LocalDateTime.now());

        if (payment.getOrder() != null && payment.getOrder().getStatus() == OrderStatus.PENDING_PAYMENT) {
            payment.getOrder().setStatus(OrderStatus.PENDING_VERIFICATION);
            payment.getOrder().setUpdatedAt(LocalDateTime.now());
        }

        return mapToSummary(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public PaymentSummaryResponse confirmFakePayosPayment(String email, PayosPaymentActionRequest request) {
        Order order = resolveOwnedOrder(email, request.getOrderId());
        Payment payment = resolvePayosPayment(order.getId());

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_CONFIRMED, "Payment already completed");
        }

        payment.setMethod(PaymentMethod.PAYOS);
        payment.setAmount(calculateFinalAmount(order));
        payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);
        payment.setTransactionCode(defaultTransactionCode(order));
        payment.setExpiredAt(resolveExpiration(payment));
        payment.setPaidAt(null);

        Payment savedPayment = paymentRepository.save(payment);
        PaymentSummaryResponse response = mapToSummary(savedPayment);
        publishPaymentEvent("payment_confirmed", order, response, "Khach hang da bao thanh toan");
        return response;
    }

    @Override
    @Transactional
    public PaymentSummaryResponse approveFakePayosPayment(String actorEmail, PayosPaymentActionRequest request) {
        Payment payment = resolvePayosPayment(request.getOrderId());
        Order order = payment.getOrder();

        if (payment.getStatus() != PaymentStatus.PENDING_CONFIRMATION) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Payment is not waiting for confirmation");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionCode(defaultTransactionCode(order));
        payment.setPaidAt(LocalDateTime.now());

        if (order != null && order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.PENDING_VERIFICATION);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
        }

        Payment savedPayment = paymentRepository.save(payment);
        PaymentSummaryResponse response = mapToSummary(savedPayment);
        publishPaymentEvent("payment_approved", order, response, "Payment approved by " + actorEmail);
        return response;
    }

    @Override
    @Transactional
    public PaymentSummaryResponse rejectFakePayosPayment(String actorEmail, PayosPaymentActionRequest request) {
        Payment payment = resolvePayosPayment(request.getOrderId());
        Order order = payment.getOrder();

        if (payment.getStatus() != PaymentStatus.PENDING_CONFIRMATION) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Payment is not waiting for confirmation");
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setPaidAt(null);

        Payment savedPayment = paymentRepository.save(payment);
        PaymentSummaryResponse response = mapToSummary(savedPayment);
        publishPaymentEvent("payment_rejected", order, response, "Payment rejected by " + actorEmail);
        return response;
    }

    @Override
    public List<PaymentSummaryResponse> getPendingFakePayosPayments() {
        return paymentRepository.findByMethodAndStatusOrderByCreatedAtDesc(PaymentMethod.PAYOS, PaymentStatus.PENDING_CONFIRMATION)
                .stream()
                .map(this::mapToSummary)
                .toList();
    }

    @Override
    public SseEmitter subscribeCustomerPaymentEvents(String email) {
        return paymentRealtimeService.subscribeCustomer(email);
    }

    @Override
    public SseEmitter subscribeStaffPaymentEvents(String email) {
        return paymentRealtimeService.subscribeStaff(email);
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
                .map(this::mapToSummary)
                .collect(Collectors.toList());
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

        Optional<Payment> existingPayment = paymentRepository.findByOrderId(orderId);
        if (existingPayment.isPresent() && existingPayment.get().getStatus() == PaymentStatus.PAID) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_CONFIRMED, "Payment already completed");
        }

        Payment payment = existingPayment.orElse(new Payment());
        payment.setOrder(order);
        payment.setAmount(calculateFinalAmount(order));
        payment.setMethod(PaymentMethod.PAYOS);
        if (payment.getStatus() == null || payment.getStatus() == PaymentStatus.UNPAID) {
            payment.setStatus(PaymentStatus.PENDING);
        }
        payment.setTransactionCode(defaultTransactionCode(order));
        payment.setExpiredAt(LocalDateTime.now().plusHours(24));
        payment.setPaidAt(null);
        paymentRepository.save(payment);

        return "http://localhost:5173/payment/payos?orderId=" + order.getId();
    }

    @Override
    @Transactional
    public void handlePayosReturn(Long orderCode) {
        Payment payment = resolvePayosPayment(orderCode);
        if (payment.getStatus() == PaymentStatus.PAID || payment.getStatus() == PaymentStatus.PENDING_CONFIRMATION) {
            return;
        }

        payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);
        payment.setTransactionCode(defaultTransactionCode(payment.getOrder()));
        payment.setPaidAt(null);

        Payment savedPayment = paymentRepository.save(payment);
        publishPaymentEvent(
                "payment_confirmed",
                payment.getOrder(),
                mapToSummary(savedPayment),
                "Customer returned from PayOS checkout"
        );
    }

    private PaymentSummaryResponse mapToSummary(Payment payment) {
        return PaymentSummaryResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder() != null ? payment.getOrder().getId() : null)
                .orderCode(payment.getOrder() != null ? payment.getOrder().getOrderCode() : null)
                .customerId(payment.getOrder() != null && payment.getOrder().getUser() != null ? payment.getOrder().getUser().getId() : null)
                .customerName(payment.getOrder() != null && payment.getOrder().getUser() != null ? payment.getOrder().getUser().getFullName() : null)
                .customerEmail(payment.getOrder() != null && payment.getOrder().getUser() != null ? payment.getOrder().getUser().getEmail() : null)
                .method(payment.getMethod())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .transactionCode(payment.getTransactionCode())
                .paymentProofImg(payment.getPaymentProofImg())
                .createdAt(payment.getCreatedAt())
                .expiredAt(payment.getExpiredAt())
                .paidAt(payment.getPaidAt())
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

    private Payment resolvePayosPayment(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND, "Payment not found"));

        if (payment.getMethod() != PaymentMethod.PAYOS) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Order is not using PAYOS payment");
        }

        return payment;
    }

    private void publishPaymentEvent(
            String eventType,
            Order order,
            PaymentSummaryResponse payment,
            String message
    ) {
        if (order == null) {
            return;
        }

        String redirectUrl = payment.getStatus() == PaymentStatus.PAID ? "/payment/success" : null;
        PaymentRealtimeEventResponse payload = PaymentRealtimeEventResponse.builder()
                .eventType(eventType)
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .paymentStatus(payment.getStatus())
                .message(message)
                .redirectUrl(redirectUrl)
                .payment(payment)
                .build();

        if (order.getUser() != null && order.getUser().getEmail() != null) {
            paymentRealtimeService.publishToCustomer(order.getUser().getEmail(), eventType, payload);
        }
        paymentRealtimeService.publishToStaff(eventType, payload);
    }

    private PaymentMethod resolveQrPaymentMethod(Payment payment) {
        if (payment != null && payment.getMethod() == PaymentMethod.PAYOS) {
            return PaymentMethod.PAYOS;
        }
        return PaymentMethod.BANK_TRANSFER;
    }

    private PaymentStatus resolveQrPaymentStatus(Payment payment) {
        if (payment != null && payment.getStatus() != null) {
            return payment.getStatus();
        }
        return PaymentStatus.PENDING;
    }

    private String buildFakeQrPayload(Order order, BigDecimal amount, String transferContent) {
        return "PAYOS-Fake"
                + "|orderId=" + (order != null ? order.getId() : "")
                + "|orderCode=" + (order != null ? order.getOrderCode() : "")
                + "|amount=" + (amount != null ? amount.toPlainString() : "0")
                + "|content=" + transferContent;
    }

    private String buildFakeQrDataUrl(Order order, BigDecimal amount, String transferContent) {
        String amountText = amount != null ? amount.toPlainString() : "0";
        String orderCode = order != null && order.getOrderCode() != null ? order.getOrderCode() : "ORD";
        String svg = """
                <svg xmlns='http://www.w3.org/2000/svg' width='360' height='420' viewBox='0 0 360 420'>
                  <rect width='360' height='420' rx='24' fill='#ffffff'/>
                  <rect x='24' y='24' width='312' height='312' rx='16' fill='#f3f7ff' stroke='#d6e4ff' stroke-width='2'/>
                  <rect x='52' y='52' width='256' height='256' fill='#ffffff'/>
                  <g fill='#111827'>
                    <rect x='68' y='68' width='56' height='56'/>
                    <rect x='76' y='76' width='40' height='40' fill='#ffffff'/>
                    <rect x='84' y='84' width='24' height='24'/>
                    <rect x='236' y='68' width='56' height='56'/>
                    <rect x='244' y='76' width='40' height='40' fill='#ffffff'/>
                    <rect x='252' y='84' width='24' height='24'/>
                    <rect x='68' y='236' width='56' height='56'/>
                    <rect x='76' y='244' width='40' height='40' fill='#ffffff'/>
                    <rect x='84' y='252' width='24' height='24'/>
                    <rect x='144' y='68' width='12' height='12'/>
                    <rect x='168' y='68' width='12' height='12'/>
                    <rect x='180' y='80' width='12' height='12'/>
                    <rect x='204' y='68' width='12' height='12'/>
                    <rect x='144' y='92' width='12' height='12'/>
                    <rect x='156' y='104' width='12' height='12'/>
                    <rect x='192' y='104' width='12' height='12'/>
                    <rect x='216' y='92' width='12' height='12'/>
                    <rect x='144' y='128' width='12' height='12'/>
                    <rect x='168' y='128' width='12' height='12'/>
                    <rect x='192' y='128' width='12' height='12'/>
                    <rect x='216' y='128' width='12' height='12'/>
                    <rect x='140' y='152' width='16' height='16'/>
                    <rect x='164' y='152' width='16' height='16'/>
                    <rect x='188' y='152' width='16' height='16'/>
                    <rect x='212' y='152' width='16' height='16'/>
                    <rect x='144' y='180' width='12' height='12'/>
                    <rect x='180' y='180' width='12' height='12'/>
                    <rect x='204' y='180' width='12' height='12'/>
                    <rect x='228' y='180' width='12' height='12'/>
                    <rect x='140' y='204' width='16' height='16'/>
                    <rect x='164' y='204' width='16' height='16'/>
                    <rect x='212' y='204' width='16' height='16'/>
                    <rect x='236' y='204' width='16' height='16'/>
                    <rect x='144' y='228' width='12' height='12'/>
                    <rect x='168' y='228' width='12' height='12'/>
                    <rect x='192' y='228' width='12' height='12'/>
                    <rect x='216' y='228' width='12' height='12'/>
                    <rect x='144' y='252' width='12' height='12'/>
                    <rect x='180' y='252' width='12' height='12'/>
                    <rect x='204' y='252' width='12' height='12'/>
                    <rect x='228' y='252' width='12' height='12'/>
                    <rect x='144' y='276' width='12' height='12'/>
                    <rect x='168' y='276' width='12' height='12'/>
                    <rect x='216' y='276' width='12' height='12'/>
                    <rect x='240' y='276' width='12' height='12'/>
                    <rect x='264' y='276' width='12' height='12'/>
                  </g>
                  <text x='180' y='356' text-anchor='middle' font-family='Arial, sans-serif' font-size='20' font-weight='700' fill='#0f172a'></text>
                  <text x='180' y='382' text-anchor='middle' font-family='Arial, sans-serif' font-size='14' fill='#334155'>%s • %s VND</text>
                  <text x='180' y='402' text-anchor='middle' font-family='Arial, sans-serif' font-size='12' fill='#64748b'>%s</text>
                </svg>
                """.formatted(escapeXml(orderCode), escapeXml(amountText), escapeXml(transferContent));

        String base64Svg = Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        return "data:image/svg+xml;base64," + base64Svg;
    }

    private String buildTransferContent(Order order) {
        if (order == null) {
            return "PAYOS";
        }

        if (order.getOrderCode() != null && !order.getOrderCode().isBlank()) {
            return "PAYOS " + order.getOrderCode().trim();
        }

        return "PAYOS ORD-" + order.getId();
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String defaultTransactionCode(Order order) {
        if (order == null || order.getId() == null) {
            return null;
        }
        return "PAYOS-" + order.getId();
    }

    private LocalDateTime resolveExpiration(Payment payment) {
        if (payment != null && payment.getExpiredAt() != null && payment.getExpiredAt().isAfter(LocalDateTime.now())) {
            return payment.getExpiredAt();
        }
        return LocalDateTime.now().plusHours(24);
    }

    private BigDecimal sumAmounts(List<Payment> payments) {
        return payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateFinalAmount(Order order) {
        if (order == null) {
            return BigDecimal.ZERO;
        }

        return (order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO)
                .add(order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO)
                .subtract(order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO);
    }

    private LocalDateTime atStartOfDayOrMin(LocalDate date) {
        return (date == null ? LocalDate.now().minusDays(29) : date).atStartOfDay();
    }

    private LocalDateTime atEndOfDayOrNow(LocalDate date) {
        LocalDate localDate = date == null ? LocalDate.now() : date;
        return localDate.atTime(23, 59, 59);
    }
}
