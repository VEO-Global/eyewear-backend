package com.veo.backend.service.impl;

import com.veo.backend.dto.request.PayosPaymentActionRequest;
import com.veo.backend.dto.response.PaymentSummaryResponse;
import com.veo.backend.entity.Order;
import com.veo.backend.entity.Payment;
import com.veo.backend.entity.User;
import com.veo.backend.enums.OrderStatus;
import com.veo.backend.enums.PaymentMethod;
import com.veo.backend.enums.PaymentStatus;
import com.veo.backend.exception.AppException;
import com.veo.backend.repository.OrderRepository;
import com.veo.backend.repository.PaymentRepository;
import com.veo.backend.repository.UserRepository;
import com.veo.backend.service.PaymentRealtimeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.payos.PayOS;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PayOS payOS;
    @Mock
    private PaymentRealtimeService paymentRealtimeService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void confirmFakePayosPayment_shouldMovePaymentToPendingConfirmation() {
        User user = buildUser(1L, "customer@example.com");
        Order order = buildOrder(100L, user, OrderStatus.PENDING_PAYMENT);
        Payment payment = buildPayment(order, PaymentStatus.PENDING);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(order.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PayosPaymentActionRequest request = new PayosPaymentActionRequest();
        request.setOrderId(order.getId());

        PaymentSummaryResponse response = paymentService.confirmFakePayosPayment(user.getEmail(), request);

        assertEquals(PaymentStatus.PENDING_CONFIRMATION, response.getStatus());
        assertEquals(PaymentStatus.PENDING_CONFIRMATION, payment.getStatus());
        verify(paymentRealtimeService).publishToStaff(eq("payment_confirmed"), any());
        verify(paymentRealtimeService).publishToCustomer(eq(user.getEmail()), eq("payment_confirmed"), any());
    }

    @Test
    void approveFakePayosPayment_shouldMarkPaidAndAdvanceOrder() {
        User user = buildUser(2L, "customer2@example.com");
        Order order = buildOrder(101L, user, OrderStatus.PENDING_PAYMENT);
        Payment payment = buildPayment(order, PaymentStatus.PENDING_CONFIRMATION);

        when(paymentRepository.findByOrderId(order.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PayosPaymentActionRequest request = new PayosPaymentActionRequest();
        request.setOrderId(order.getId());

        PaymentSummaryResponse response = paymentService.approveFakePayosPayment("sale@example.com", request);

        assertEquals(PaymentStatus.PAID, response.getStatus());
        assertEquals(OrderStatus.PENDING_VERIFICATION, order.getStatus());
        verify(paymentRealtimeService).publishToStaff(eq("payment_approved"), any());
        verify(paymentRealtimeService).publishToCustomer(eq(user.getEmail()), eq("payment_approved"), any());
    }

    @Test
    void rejectFakePayosPayment_shouldMarkFailedWithoutAdvancingOrder() {
        User user = buildUser(3L, "customer3@example.com");
        Order order = buildOrder(102L, user, OrderStatus.PENDING_PAYMENT);
        Payment payment = buildPayment(order, PaymentStatus.PENDING_CONFIRMATION);

        when(paymentRepository.findByOrderId(order.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PayosPaymentActionRequest request = new PayosPaymentActionRequest();
        request.setOrderId(order.getId());

        PaymentSummaryResponse response = paymentService.rejectFakePayosPayment("sale@example.com", request);

        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRealtimeService).publishToStaff(eq("payment_rejected"), any());
        verify(paymentRealtimeService).publishToCustomer(eq(user.getEmail()), eq("payment_rejected"), any());
    }

    @Test
    void getPendingFakePayosPayments_shouldReturnOnlyPendingConfirmationPayos() {
        User user = buildUser(4L, "customer4@example.com");
        Order order = buildOrder(103L, user, OrderStatus.PENDING_PAYMENT);
        Payment payment = buildPayment(order, PaymentStatus.PENDING_CONFIRMATION);

        when(paymentRepository.findByMethodAndStatusOrderByCreatedAtDesc(PaymentMethod.PAYOS, PaymentStatus.PENDING_CONFIRMATION))
                .thenReturn(List.of(payment));

        List<PaymentSummaryResponse> responses = paymentService.getPendingFakePayosPayments();

        assertEquals(1, responses.size());
        assertEquals(PaymentStatus.PENDING_CONFIRMATION, responses.getFirst().getStatus());
        assertEquals(order.getId(), responses.getFirst().getOrderId());
    }

    @Test
    void confirmFakePayosPayment_shouldRejectNonPayosOrder() {
        User user = buildUser(5L, "customer5@example.com");
        Order order = buildOrder(104L, user, OrderStatus.PENDING_PAYMENT);
        Payment payment = buildPayment(order, PaymentStatus.PENDING);
        payment.setMethod(PaymentMethod.COD);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(order.getId())).thenReturn(Optional.of(payment));

        PayosPaymentActionRequest request = new PayosPaymentActionRequest();
        request.setOrderId(order.getId());

        AppException exception = assertThrows(
                AppException.class,
                () -> paymentService.confirmFakePayosPayment(user.getEmail(), request)
        );

        assertEquals("Order is not using PAYOS payment", exception.getMessage());
    }

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFullName("Customer " + id);
        return user;
    }

    private Order buildOrder(Long id, User user, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setOrderCode("ORD-" + id);
        order.setUser(user);
        order.setStatus(status);
        order.setTotalAmount(BigDecimal.valueOf(1000000));
        order.setShippingFee(BigDecimal.valueOf(30000));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        order.setUpdatedAt(order.getCreatedAt());
        return order;
    }

    private Payment buildPayment(Order order, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(order.getId() + 1000);
        payment.setOrder(order);
        payment.setMethod(PaymentMethod.PAYOS);
        payment.setStatus(status);
        payment.setAmount(BigDecimal.valueOf(1030000));
        payment.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        payment.setExpiredAt(LocalDateTime.now().plusHours(24));
        return payment;
    }
}
