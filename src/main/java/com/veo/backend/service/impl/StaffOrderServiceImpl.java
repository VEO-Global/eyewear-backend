package com.veo.backend.service.impl;

import com.veo.backend.dto.request.StaffOrderPhaseUpdateRequest;
import com.veo.backend.dto.response.*;
import com.veo.backend.entity.*;
import com.veo.backend.enums.*;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.OrderRepository;
import com.veo.backend.repository.OrderStatusHistoryRepository;
import com.veo.backend.repository.PaymentRepository;
import com.veo.backend.repository.PrescriptionRepository;
import com.veo.backend.service.StaffOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StaffOrderServiceImpl implements StaffOrderService {
    private final OrderRepository orderRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StaffOrderResponse> getOrders(String phase, String status) {
        List<Order> orders = loadOrders(status);
        Map<Long, Prescription> prescriptionsByOrderId = getPrescriptionsByOrderId(extractOrderIds(orders));
        Map<Long, Payment> latestPaymentsByOrderId = getLatestPaymentsByOrderId(extractOrderIds(orders));

        return orders.stream()
                .filter(order -> shouldIncludeInStatusFilter(status, order, prescriptionsByOrderId.get(order.getId())))
                .map(order -> toStaffOrderResponse(
                        order,
                        prescriptionsByOrderId.get(order.getId()),
                        latestPaymentsByOrderId.get(order.getId())
                ))
                .filter(response -> matchesPhase(response, phase))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StaffOrderResponse getOrderDetail(Long id) {
        Order order = orderRepository.findDetailedById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));
        Prescription prescription = prescriptionRepository.findFirstByOrderIdOrderByIdDesc(order.getId()).orElse(null);
        Payment payment = paymentRepository.findFirstByOrderIdOrderByIdDesc(order.getId()).orElse(null);
        return toStaffOrderResponse(order, prescription, payment);
    }

    @Override
    @Transactional
    public StaffOrderResponse updateOrderPhase(Long id, String actorEmail, StaffOrderPhaseUpdateRequest request) {
        Order order = orderRepository.findDetailedById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));
        Prescription prescription = prescriptionRepository.findFirstByOrderIdOrderByIdDesc(order.getId()).orElse(null);

        if (request.getPhase() == StaffOrderPhase.READY_TO_DELIVER) {
            validateReadyForHandoff(order, prescription);
        }

        OrderStatus targetStatus = resolveTargetStatus(order, prescription, request.getPhase());
        validateTransition(order.getStatus(), targetStatus);
        applyStatus(order, targetStatus, actorEmail, request.getNote());

        if (prescription != null
                && request.getPhase() == StaffOrderPhase.PRESCRIPTION_REVIEW
                && prescription.getReviewStatus() == null) {
            prescription.setReviewStatus(PrescriptionReviewStatus.PENDING);
            prescriptionRepository.save(prescription);
        }

        Order savedOrder = orderRepository.save(order);
        Payment payment = paymentRepository.findFirstByOrderIdOrderByIdDesc(savedOrder.getId()).orElse(null);
        return toStaffOrderResponse(savedOrder, prescription, payment);
    }

    @Override
    @Transactional
    public StaffOrderResponse confirmOrder(Long id, String actorEmail) {
        StaffOrderPhaseUpdateRequest request = new StaffOrderPhaseUpdateRequest();
        request.setPhase(StaffOrderPhase.PENDING_CONFIRMATION);
        request.setNote("Confirmed by staff");
        return updateOrderPhase(id, actorEmail, request);
    }

    @Override
    @Transactional
    public StaffOrderResponse handoffOrder(Long id, String actorEmail) {
        Order order = orderRepository.findDetailedById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));
        Prescription prescription = prescriptionRepository.findFirstByOrderIdOrderByIdDesc(order.getId()).orElse(null);

        validateReadyForHandoff(order, prescription);

        if (OrderFlowStateResolver.resolvePhase(order, prescription) != StaffOrderPhase.READY_TO_DELIVER) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "Order is not ready to hand off to operations"
            );
        }

        OrderStatus targetStatus = resolveTargetStatus(order, prescription, StaffOrderPhase.READY_TO_DELIVER);
        validateTransition(order.getStatus(), targetStatus);
        applyStatus(order, targetStatus, actorEmail, "Handed off to operations");

        Order savedOrder = orderRepository.save(order);
        Payment payment = paymentRepository.findFirstByOrderIdOrderByIdDesc(savedOrder.getId()).orElse(null);
        return toStaffOrderResponse(savedOrder, prescription, payment);
    }

    @Override
    @Transactional
    public StaffOrderResponse completeOrder(Long id, String actorEmail) {
        StaffOrderPhaseUpdateRequest request = new StaffOrderPhaseUpdateRequest();
        request.setPhase(StaffOrderPhase.COMPLETED);
        request.setNote("Completed by staff");
        return updateOrderPhase(id, actorEmail, request);
    }

    private List<Order> loadOrders(String status) {
        if (status == null || status.isBlank()) {
            return deduplicateOrders(orderRepository.findAllByOrderByCreatedAtDesc());
        }

        OrderStatus orderStatus;
        try {
            orderStatus = OrderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Invalid order status");
        }

        return deduplicateOrders(orderRepository.findAllByStatusInOrderByCreatedAtDesc(List.of(orderStatus)));
    }

    private List<Order> deduplicateOrders(List<Order> orders) {
        LinkedHashMap<Long, Order> deduplicated = new LinkedHashMap<>();
        for (Order order : orders) {
            deduplicated.put(order.getId(), order);
        }
        return new ArrayList<>(deduplicated.values());
    }

    private boolean matchesPhase(StaffOrderResponse response, String phase) {
        if (phase == null || phase.isBlank()) {
            return true;
        }

        try {
            StaffOrderPhase expectedPhase = StaffOrderPhase.valueOf(phase.trim().toUpperCase(Locale.ROOT));
            return expectedPhase == response.getPhase();
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Invalid staff order phase");
        }
    }

    private boolean shouldIncludeInStatusFilter(String status, Order order, Prescription prescription) {
        if (status == null || status.isBlank()) {
            return true;
        }

        OrderStatus requestedStatus;
        try {
            requestedStatus = OrderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Invalid order status");
        }

        if (requestedStatus != OrderStatus.PENDING_VERIFICATION) {
            return true;
        }

        return OrderFlowStateResolver.isReadyToDeliver(order, prescription);
    }

    private StaffOrderResponse toStaffOrderResponse(Order order, Prescription prescription, Payment payment) {
        boolean requiresPrescription = OrderFlowStateResolver.requiresPrescription(order, prescription);
        PrescriptionReviewStatus effectiveReviewStatus = OrderFlowStateResolver.resolveEffectiveReviewStatus(order, prescription);

        return StaffOrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderCode())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt())
                .phase(OrderFlowStateResolver.resolvePhase(order, prescription))
                .phaseLabel(OrderFlowStateResolver.resolvePhaseLabel(order, prescription))
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .paymentStatus(payment != null ? payment.getStatus() : PaymentStatus.UNPAID)
                .totalAmount(defaultAmount(order.getTotalAmount()).add(defaultAmount(order.getShippingFee())).subtract(defaultAmount(order.getDiscountAmount())))
                .receiverName(order.getReceiverName())
                .phoneNumber(order.getPhoneNumber())
                .note(order.getNote())
                .requiresPrescription(requiresPrescription)
                .prescriptionOption(order.getPrescriptionOption())
                .prescriptionReviewStatus(effectiveReviewStatus)
                .customerProfile(mapCustomerProfile(order.getUser()))
                .shippingAddress(order.getShippingAddress())
                .sourceChannel("ONLINE")
                .lensProduct(mapLensSummaryResponse(prescription))
                .prescription(mapPrescriptionResponse(order, prescription))
                .payment(mapPaymentSummary(payment))
                .items(mapOrderItems(order.getItems()))
                .build();
    }

    private List<Long> extractOrderIds(List<Order> orders) {
        return orders.stream()
                .map(Order::getId)
                .toList();
    }

    private Map<Long, Prescription> getPrescriptionsByOrderId(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Prescription> prescriptionsByOrderId = new HashMap<>();
        for (Prescription prescription : prescriptionRepository.findByOrderIdIn(orderIds)) {
            if (prescription.getOrder() != null && prescription.getOrder().getId() != null) {
                Long orderId = prescription.getOrder().getId();
                Prescription current = prescriptionsByOrderId.get(orderId);
                if (current == null
                        || (prescription.getId() != null && current.getId() != null && prescription.getId() > current.getId())
                        || current.getId() == null) {
                    prescriptionsByOrderId.put(orderId, prescription);
                }
            }
        }
        return prescriptionsByOrderId;
    }

    private Map<Long, Payment> getLatestPaymentsByOrderId(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Payment> latestPaymentsByOrderId = new HashMap<>();
        for (Payment payment : paymentRepository.findByOrderIdIn(orderIds)) {
            if (payment.getOrder() == null || payment.getOrder().getId() == null) {
                continue;
            }

            Long orderId = payment.getOrder().getId();
            Payment current = latestPaymentsByOrderId.get(orderId);
            if (current == null || (payment.getId() != null && current.getId() != null && payment.getId() > current.getId())) {
                latestPaymentsByOrderId.put(orderId, payment);
            } else if (current == null) {
                latestPaymentsByOrderId.put(orderId, payment);
            }
        }
        return latestPaymentsByOrderId;
    }

    private StaffOrderCustomerProfileResponse mapCustomerProfile(User user) {
        if (user == null) {
            return null;
        }

        return StaffOrderCustomerProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();
    }

    private PaymentSummaryResponse mapPaymentSummary(Payment payment) {
        if (payment == null) {
            return null;
        }

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

    private List<OrderItemResponse> mapOrderItems(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return List.of();
        }

        return orderItems.stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .orderItemId(item.getId())
                        .productVariantId(item.getProductVariant() != null ? item.getProductVariant().getId() : null)
                        .productId(item.getProductVariant() != null && item.getProductVariant().getProduct() != null
                                ? item.getProductVariant().getProduct().getId() : null)
                        .productName(item.getProductVariant() != null && item.getProductVariant().getProduct() != null
                                ? item.getProductVariant().getProduct().getName() : null)
                        .productVariantName(resolveVariantName(item))
                        .variantName(resolveVariantName(item))
                        .lensProductId(item.getLensProduct() != null ? item.getLensProduct().getId() : null)
                        .lensProductName(item.getLensProduct() != null ? item.getLensProduct().getName() : null)
                        .quantity(item.getQuantity())
                        .unitPrice(resolveUnitPrice(item))
                        .lineTotal(item.getPrice())
                        .price(item.getPrice())
                        .thumbnailUrl(null)
                        .build())
                .toList();
    }

    private LensSummaryResponse mapLensSummaryResponse(Prescription prescription) {
        if (prescription == null) {
            return null;
        }

        return LensSummaryResponse.builder()
                .id(prescription.getLensProduct() != null ? prescription.getLensProduct().getId() : null)
                .name(prescription.getLensNameSnapshot() != null
                        ? prescription.getLensNameSnapshot()
                        : prescription.getLensProduct() != null ? prescription.getLensProduct().getName() : null)
                .price(prescription.getLensPriceSnapshot() != null
                        ? prescription.getLensPriceSnapshot()
                        : prescription.getLensProduct() != null ? prescription.getLensProduct().getPrice() : null)
                .description(prescription.getLensDescriptionSnapshot() != null
                        ? prescription.getLensDescriptionSnapshot()
                        : prescription.getLensProduct() != null ? prescription.getLensProduct().getDescription() : null)
                .build();
    }

    private PrescriptionResponse mapPrescriptionResponse(Order order, Prescription prescription) {
        if (prescription == null) {
            return null;
        }

        return PrescriptionResponse.builder()
                .prescriptionImageUrl(prescription.getPrescriptionImageUrl())
                .sphereOd(prescription.getSphereOd())
                .sphereOs(prescription.getSphereOs())
                .cylinderOd(prescription.getCylinderOd())
                .cylinderOs(prescription.getCylinderOs())
                .axisOd(prescription.getAxisOd())
                .axisOs(prescription.getAxisOs())
                .pd(prescription.getPd())
                .reviewStatus(OrderFlowStateResolver.resolveEffectiveReviewStatus(order, prescription))
                .reviewNote(prescription.getReviewNote())
                .build();
    }

    private OrderStatus resolveTargetStatus(Order order, Prescription prescription, StaffOrderPhase phase) {
        return switch (phase) {
            case PENDING_CONFIRMATION -> OrderStatus.PENDING_VERIFICATION;
            case PRESCRIPTION_REVIEW -> {
                if (!OrderFlowStateResolver.requiresPrescription(order, prescription)) {
                    throw new AppException(ErrorCode.VALIDATION_ERROR, "Order does not require prescription review");
                }
                yield OrderStatus.PENDING_VERIFICATION;
            }
            case PROCESSING -> OrderStatus.PENDING_VERIFICATION;
            case READY_TO_DELIVER -> resolveReadyToDeliverStatus(order, prescription);
            case SHIPPING -> OrderStatus.SHIPPING;
            case COMPLETED -> OrderStatus.COMPLETED;
            case CANCELED -> OrderStatus.CANCELLED;
            case RETURN_REFUND -> throw new AppException(ErrorCode.VALIDATION_ERROR, "Return/refund flow is not implemented for staff orders");
        };
    }

    private void validateReadyForHandoff(Order order, Prescription prescription) {
        if (order == null) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found");
        }

        if (OrderFlowStateResolver.requiresPrescription(order, prescription)
                && OrderFlowStateResolver.resolveEffectiveReviewStatus(order, prescription) != PrescriptionReviewStatus.APPROVED) {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "Prescription must be approved before handoff to operations"
            );
        }
    }

    private OrderStatus resolveReadyToDeliverStatus(Order order, Prescription prescription) {
        if (order == null) {
            return OrderStatus.PENDING_VERIFICATION;
        }

        if (order.getOrderType() == OrderType.PRE_ORDER) {
            return OrderStatus.WAITING_FOR_STOCK;
        }

        if (OrderFlowStateResolver.requiresPrescription(order, prescription)) {
            return OrderStatus.MANUFACTURING;
        }

        return OrderStatus.PACKING;
    }

    private void validateTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return;
        }

        if (currentStatus == OrderStatus.COMPLETED) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Completed order cannot be updated");
        }

        if (currentStatus == OrderStatus.CANCELLED) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Canceled order cannot be updated");
        }

        if (targetStatus == OrderStatus.PENDING_VERIFICATION
                && currentStatus != OrderStatus.PENDING_PAYMENT
                && currentStatus != OrderStatus.PENDING_VERIFICATION) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Only pending orders can move to confirmation/review");
        }

        if (targetStatus == OrderStatus.MANUFACTURING
                && currentStatus != OrderStatus.PENDING_VERIFICATION
                && currentStatus != OrderStatus.WAITING_FOR_STOCK
                && currentStatus != OrderStatus.MANUFACTURING) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Order must be confirmed before processing");
        }

        if (targetStatus == OrderStatus.WAITING_FOR_STOCK
                && currentStatus != OrderStatus.PENDING_PAYMENT
                && currentStatus != OrderStatus.PENDING_VERIFICATION
                && currentStatus != OrderStatus.WAITING_FOR_STOCK) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Only newly confirmed orders can be handed off to operations");
        }

        if (targetStatus == OrderStatus.PACKING
                && currentStatus != OrderStatus.PENDING_VERIFICATION
                && currentStatus != OrderStatus.WAITING_FOR_STOCK
                && currentStatus != OrderStatus.MANUFACTURING
                && currentStatus != OrderStatus.PACKING) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Order must be confirmed before packing");
        }

        if (targetStatus == OrderStatus.READY_TO_SHIP
                && currentStatus != OrderStatus.PACKING
                && currentStatus != OrderStatus.MANUFACTURING
                && currentStatus != OrderStatus.READY_TO_SHIP) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Order must be processed before it can be marked ready to ship");
        }

        if (targetStatus == OrderStatus.SHIPPING
                && currentStatus != OrderStatus.PACKING
                && currentStatus != OrderStatus.READY_TO_SHIP
                && currentStatus != OrderStatus.SHIPPING) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Order must be in processing before shipping");
        }

        if (targetStatus == OrderStatus.COMPLETED && currentStatus != OrderStatus.SHIPPING) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Only shipping orders can be completed");
        }
    }

    private void applyStatus(Order order, OrderStatus targetStatus, String actorEmail, String note) {
        order.setStatus(targetStatus);
        order.setUpdatedAt(LocalDateTime.now());

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(targetStatus);
        history.setNote(buildHistoryNote(actorEmail, note));
        history.setCreatedAt(order.getUpdatedAt());
        orderStatusHistoryRepository.save(history);
    }

    private String buildHistoryNote(String actorEmail, String note) {
        List<String> parts = new ArrayList<>();
        if (actorEmail != null && !actorEmail.isBlank()) {
            parts.add("Updated by " + actorEmail.trim());
        }
        if (note != null && !note.isBlank()) {
            parts.add(note.trim());
        }
        return parts.isEmpty() ? null : String.join(" | ", parts);
    }

    private String resolveVariantName(OrderItem item) {
        if (item == null || item.getProductVariant() == null) {
            return null;
        }

        ProductVariant variant = item.getProductVariant();
        Product product = variant.getProduct();
        List<String> parts = new ArrayList<>();

        if (product != null && product.getName() != null) {
            parts.add(product.getName());
        }
        if (variant.getColor() != null && !variant.getColor().isBlank()) {
            parts.add(variant.getColor());
        }
        if (variant.getSize() != null && !variant.getSize().isBlank()) {
            parts.add("Size " + variant.getSize());
        }

        if (parts.isEmpty()) {
            return variant.getSku();
        }

        return String.join(" - ", parts);
    }

    private BigDecimal resolveUnitPrice(OrderItem item) {
        if (item == null || item.getQuantity() == null || item.getQuantity() <= 0 || item.getPrice() == null) {
            return null;
        }

        return item.getPrice().divide(BigDecimal.valueOf(item.getQuantity()), 2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
