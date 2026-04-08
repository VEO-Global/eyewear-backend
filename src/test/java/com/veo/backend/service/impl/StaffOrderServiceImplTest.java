package com.veo.backend.service.impl;

import com.veo.backend.dto.response.OrderResponse;
import com.veo.backend.dto.response.StaffOrderResponse;
import com.veo.backend.dto.request.StaffOrderPhaseUpdateRequest;
import com.veo.backend.entity.Order;
import com.veo.backend.entity.Prescription;
import com.veo.backend.entity.User;
import com.veo.backend.enums.OrderStatus;
import com.veo.backend.enums.OrderType;
import com.veo.backend.enums.PrescriptionOption;
import com.veo.backend.enums.PrescriptionReviewStatus;
import com.veo.backend.enums.StaffOrderPhase;
import com.veo.backend.exception.AppException;
import com.veo.backend.repository.OrderRepository;
import com.veo.backend.repository.OrderStatusHistoryRepository;
import com.veo.backend.repository.PaymentRepository;
import com.veo.backend.repository.PrescriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffOrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PrescriptionRepository prescriptionRepository;
    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private StaffOrderServiceImpl staffOrderService;

    @Test
    void handoffOrder_shouldMoveReadyOrderIntoOperationsFlow() {
        Order order = buildOrder(138L, "ORD-138", OrderStatus.PENDING_VERIFICATION, OrderType.NORMAL);

        when(orderRepository.findDetailedById(order.getId())).thenReturn(Optional.of(order));
        when(prescriptionRepository.findFirstByOrderIdOrderByIdDesc(order.getId())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.findFirstByOrderIdOrderByIdDesc(order.getId())).thenReturn(Optional.empty());

        StaffOrderResponse response = staffOrderService.handoffOrder(order.getId(), "staff@veo.com");

        assertEquals("PACKING", response.getStatus());
        assertEquals(StaffOrderPhase.PROCESSING, response.getPhase());
        assertEquals(OrderStatus.PACKING, order.getStatus());

        verify(orderStatusHistoryRepository).save(any());
        verify(orderRepository).save(order);
    }

    @Test
    void handoffOrder_shouldRejectOrderThatIsNotReadyToDeliver() {
        Order order = buildOrder(133L, "ORD-133", OrderStatus.PENDING_PAYMENT, OrderType.NORMAL);

        when(orderRepository.findDetailedById(order.getId())).thenReturn(Optional.of(order));
        when(prescriptionRepository.findFirstByOrderIdOrderByIdDesc(order.getId())).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> staffOrderService.handoffOrder(order.getId(), "staff@veo.com")
        );

        assertEquals("Order is not ready to hand off to operations", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void handoffOrder_shouldExplainWhenPrescriptionIsNotApproved() {
        Order order = buildOrder(145L, "ORD-145", OrderStatus.PENDING_VERIFICATION, OrderType.PRESCRIPTION);
        order.setPrescriptionOption(PrescriptionOption.WITH_PRESCRIPTION);

        Prescription prescription = new Prescription();
        prescription.setReviewStatus(PrescriptionReviewStatus.PENDING);

        when(orderRepository.findDetailedById(order.getId())).thenReturn(Optional.of(order));
        when(prescriptionRepository.findFirstByOrderIdOrderByIdDesc(order.getId())).thenReturn(Optional.of(prescription));

        AppException exception = assertThrows(
                AppException.class,
                () -> staffOrderService.handoffOrder(order.getId(), "staff@veo.com")
        );

        assertEquals("Prescription must be approved before handoff to operations", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void operationOrders_shouldIncludeHandoffStatusesAndExposeOrderPhase() {
        Order order = buildOrder(138L, "ORD-138", OrderStatus.PACKING, OrderType.NORMAL);

        OperationOrderServiceImpl operationOrderService = new OperationOrderServiceImpl(
                orderRepository,
                prescriptionRepository,
                paymentRepository,
                orderStatusHistoryRepository,
                null
        );

        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(order));
        when(orderStatusHistoryRepository.findOrderIdsByHandoffKeyword(List.of(order.getId()), "handed off to operations"))
                .thenReturn(Set.of(order.getId()));
        when(prescriptionRepository.findByOrderIdIn(List.of(order.getId()))).thenReturn(List.of());
        when(paymentRepository.findByOrderIdIn(List.of(order.getId()))).thenReturn(List.of());
        when(orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAscIdAsc(order.getId())).thenReturn(List.of());

        List<OrderResponse> responses = operationOrderService.getOrders(null, null, null);

        assertEquals(1, responses.size());
        assertEquals(OrderStatus.PACKING, responses.getFirst().getStatus());
        assertEquals(StaffOrderPhase.PROCESSING, responses.getFirst().getOrderPhase());
    }

    @Test
    void operationOrders_shouldExcludeOrdersWithoutHandoffMarker() {
        Order order = buildOrder(139L, "ORD-139", OrderStatus.PACKING, OrderType.NORMAL);

        OperationOrderServiceImpl operationOrderService = new OperationOrderServiceImpl(
                orderRepository,
                prescriptionRepository,
                paymentRepository,
                orderStatusHistoryRepository,
                null
        );

        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(order));
        when(orderStatusHistoryRepository.findOrderIdsByHandoffKeyword(List.of(order.getId()), "handed off to operations"))
                .thenReturn(Set.of());
        when(prescriptionRepository.findByOrderIdIn(List.of(order.getId()))).thenReturn(List.of());
        when(paymentRepository.findByOrderIdIn(List.of(order.getId()))).thenReturn(List.of());

        List<OrderResponse> responses = operationOrderService.getOrders(null, null, null);

        assertEquals(0, responses.size());
    }

    @Test
    void updateOrderPhase_processing_shouldKeepOrderWaitingForHandoff() {
        Order order = buildOrder(150L, "ORD-150", OrderStatus.PENDING_VERIFICATION, OrderType.PRESCRIPTION);
        order.setPrescriptionOption(PrescriptionOption.WITH_PRESCRIPTION);

        Prescription prescription = new Prescription();
        prescription.setReviewStatus(PrescriptionReviewStatus.APPROVED);

        StaffOrderPhaseUpdateRequest request = new StaffOrderPhaseUpdateRequest();
        request.setPhase(StaffOrderPhase.PROCESSING);
        request.setNote("Done prescription review");

        when(orderRepository.findDetailedById(order.getId())).thenReturn(Optional.of(order));
        when(prescriptionRepository.findFirstByOrderIdOrderByIdDesc(order.getId())).thenReturn(Optional.of(prescription));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.findFirstByOrderIdOrderByIdDesc(order.getId())).thenReturn(Optional.empty());

        StaffOrderResponse response = staffOrderService.updateOrderPhase(order.getId(), "staff@veo.com", request);

        assertEquals(OrderStatus.PENDING_VERIFICATION, order.getStatus());
        assertEquals("PENDING_VERIFICATION", response.getStatus());
        assertEquals(StaffOrderPhase.READY_TO_DELIVER, response.getPhase());
    }

    private Order buildOrder(Long id, String orderCode, OrderStatus status, OrderType orderType) {
        User user = User.builder()
                .id(1L)
                .email("customer@example.com")
                .fullName("Customer")
                .phone("0900000000")
                .build();

        Order order = new Order();
        order.setId(id);
        order.setOrderCode(orderCode);
        order.setUser(user);
        order.setStatus(status);
        order.setOrderType(orderType);
        order.setPrescriptionOption(PrescriptionOption.WITHOUT_PRESCRIPTION);
        order.setTotalAmount(BigDecimal.valueOf(1000000));
        order.setShippingFee(BigDecimal.valueOf(30000));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setReceiverName("Receiver");
        order.setPhoneNumber("0900000000");
        order.setShippingAddress("123 Test Street");
        order.setCreatedAt(LocalDateTime.of(2026, 4, 8, 10, 0));
        order.setUpdatedAt(order.getCreatedAt());
        order.setItems(List.of());
        return order;
    }
}
