package com.veo.backend.service.impl;

import com.veo.backend.dto.request.AssignLogisticsRequest;
import com.veo.backend.dto.request.OperationReceiveStockRequest;
import com.veo.backend.dto.request.OperationStatusUpdateRequest;
import com.veo.backend.dto.request.OrderTrackingRequest;
import com.veo.backend.dto.response.*;
import com.veo.backend.entity.*;
import com.veo.backend.enums.*;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.*;
import com.veo.backend.service.OperationOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OperationOrderServiceImpl implements OperationOrderService {
    private static final Set<OrderStatus> OPERATION_STATUSES = EnumSet.of(
            OrderStatus.WAITING_FOR_STOCK,
            OrderStatus.MANUFACTURING,
            OrderStatus.PACKING,
            OrderStatus.READY_TO_SHIP,
            OrderStatus.SHIPPING,
            OrderStatus.COMPLETED
    );

    private final OrderRepository orderRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(String orderType, String status, String keyword) {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        Map<Long, Prescription> prescriptionsByOrderId = getPrescriptionsByOrderId(extractOrderIds(orders));
        Map<Long, Payment> latestPaymentsByOrderId = getLatestPaymentsByOrderId(extractOrderIds(orders));

        OrderType expectedType = parseOrderType(orderType);
        OrderStatus expectedStatus = parseOrderStatus(status);
        String normalizedKeyword = keyword == null ? null : keyword.trim().toLowerCase(Locale.ROOT);

        return orders.stream()
                .filter(this::isOperationOrder)
                .filter(order -> expectedType == null || order.getOrderType() == expectedType)
                .filter(order -> expectedStatus == null || order.getStatus() == expectedStatus)
                .filter(order -> matchesKeyword(order, normalizedKeyword))
                .map(order -> toOrderResponse(
                        order,
                        prescriptionsByOrderId.get(order.getId()),
                        latestPaymentsByOrderId.get(order.getId())
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OperationOrderSummaryResponse getOrderSummary() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(this::isOperationOrder)
                .toList();

        return OperationOrderSummaryResponse.builder()
                .totalOrders(orders.size())
                .waitingForStock(countByStatus(orders, OrderStatus.WAITING_FOR_STOCK))
                .manufacturing(countByStatus(orders, OrderStatus.MANUFACTURING))
                .packing(countByStatus(orders, OrderStatus.PACKING))
                .readyToShip(countByStatus(orders, OrderStatus.READY_TO_SHIP))
                .shipping(countByStatus(orders, OrderStatus.SHIPPING))
                .completed(countByStatus(orders, OrderStatus.COMPLETED))
                .normalOrders(countByType(orders, OrderType.NORMAL))
                .prescriptionOrders(countByType(orders, OrderType.PRESCRIPTION))
                .preorderOrders(countByType(orders, OrderType.PRE_ORDER))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(Long id) {
        Order order = orderRepository.findDetailedById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));
        Prescription prescription = prescriptionRepository.findByOrderId(order.getId()).orElse(null);
        Payment payment = paymentRepository.findFirstByOrderIdOrderByIdDesc(order.getId()).orElse(null);
        return toOrderResponse(order, prescription, payment);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, String actorEmail, OperationStatusUpdateRequest request) {
        Order order = orderRepository.findDetailedById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));
        OrderStatus targetStatus = request.getStatus();

        if (!OPERATION_STATUSES.contains(targetStatus) || targetStatus == OrderStatus.WAITING_FOR_STOCK) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Unsupported operation status");
        }

        validateOperationStatusTransition(order, targetStatus);
        applyStatus(order, targetStatus, actorEmail, request.getNote());

        Order savedOrder = orderRepository.save(order);
        Prescription prescription = prescriptionRepository.findByOrderId(savedOrder.getId()).orElse(null);
        Payment payment = paymentRepository.findFirstByOrderIdOrderByIdDesc(savedOrder.getId()).orElse(null);
        return toOrderResponse(savedOrder, prescription, payment);
    }

    @Override
    @Transactional
    public OrderResponse assignLogistics(Long id, String actorEmail, AssignLogisticsRequest request) {
        Order order = orderRepository.findDetailedById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));

        validateOrderIsActive(order);

        order.setLogisticsProvider(normalizeNullable(request.getCarrier()));
        order.setShippingMethod(normalizeNullable(request.getShippingMethod()));
        order.setEstimatedDeliveryDate(request.getEstimatedDeliveryDate());
        order.setUpdatedAt(LocalDateTime.now());

        saveHistory(order, actorEmail, buildLogisticsNote(request));

        Order savedOrder = orderRepository.save(order);
        Prescription prescription = prescriptionRepository.findByOrderId(savedOrder.getId()).orElse(null);
        Payment payment = paymentRepository.findFirstByOrderIdOrderByIdDesc(savedOrder.getId()).orElse(null);
        return toOrderResponse(savedOrder, prescription, payment);
    }

    @Override
    @Transactional
    public OrderResponse updateTracking(Long id, String actorEmail, OrderTrackingRequest request) {
        Order order = orderRepository.findDetailedById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));

        validateOrderIsActive(order);

        order.setTrackingNumber(request.getTrackingNumber().trim());
        order.setLogisticsProvider(request.getProvider().trim());
        order.setUpdatedAt(LocalDateTime.now());

        saveHistory(order, actorEmail, "Tracking updated: " + request.getTrackingNumber().trim());

        Order savedOrder = orderRepository.save(order);
        Prescription prescription = prescriptionRepository.findByOrderId(savedOrder.getId()).orElse(null);
        Payment payment = paymentRepository.findFirstByOrderIdOrderByIdDesc(savedOrder.getId()).orElse(null);
        return toOrderResponse(savedOrder, prescription, payment);
    }

    @Override
    @Transactional
    public OrderResponse receivePreOrderStock(Long id, String actorEmail, OperationReceiveStockRequest request) {
        Order order = orderRepository.findDetailedById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));

        if (order.getOrderType() != OrderType.PRE_ORDER) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Only pre-order items can receive stock");
        }

        if (order.getStatus() != OrderStatus.WAITING_FOR_STOCK) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Pre-order must be waiting for stock");
        }

        Map<Long, OrderItem> orderItemsByVariantId = new HashMap<>();
        for (OrderItem item : order.getItems()) {
            if (item.getProductVariant() != null && item.getProductVariant().getId() != null) {
                orderItemsByVariantId.put(item.getProductVariant().getId(), item);
            }
        }

        for (OperationReceiveStockRequest.Item item : request.getItems()) {
            OrderItem orderItem = orderItemsByVariantId.get(item.getVariantId());
            if (orderItem == null) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Variant does not belong to this order");
            }

            ProductVariant variant = productVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND, "Product variant not found"));

            int currentStock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
            variant.setStockQuantity(currentStock + item.getReceivedQuantity());
            productVariantRepository.save(variant);
        }

        applyStatus(order, OrderStatus.PACKING, actorEmail, defaultIfBlank(request.getNote(), "Pre-order stock received"));

        Order savedOrder = orderRepository.save(order);
        Prescription prescription = prescriptionRepository.findByOrderId(savedOrder.getId()).orElse(null);
        Payment payment = paymentRepository.findFirstByOrderIdOrderByIdDesc(savedOrder.getId()).orElse(null);
        return toOrderResponse(savedOrder, prescription, payment);
    }

    private boolean isOperationOrder(Order order) {
        return order != null && order.getStatus() != null && OPERATION_STATUSES.contains(order.getStatus());
    }

    private long countByStatus(List<Order> orders, OrderStatus status) {
        return orders.stream().filter(order -> order.getStatus() == status).count();
    }

    private long countByType(List<Order> orders, OrderType type) {
        return orders.stream().filter(order -> order.getOrderType() == type).count();
    }

    private OrderType parseOrderType(String orderType) {
        if (orderType == null || orderType.isBlank()) {
            return null;
        }

        try {
            return OrderType.valueOf(orderType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Invalid order type");
        }
    }

    private OrderStatus parseOrderStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            OrderStatus parsed = OrderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            if (!OPERATION_STATUSES.contains(parsed)) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Status is not supported by operations");
            }
            return parsed;
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Invalid order status");
        }
    }

    private boolean matchesKeyword(Order order, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        return contains(order.getOrderCode(), keyword)
                || contains(order.getReceiverName(), keyword)
                || contains(order.getPhoneNumber(), keyword)
                || contains(order.getTrackingNumber(), keyword)
                || contains(order.getUser() != null ? order.getUser().getEmail() : null, keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private void validateOperationStatusTransition(Order order, OrderStatus targetStatus) {
        validateOrderIsActive(order);

        OrderStatus currentStatus = order.getStatus();
        if (currentStatus == targetStatus) {
            return;
        }

        switch (targetStatus) {
            case MANUFACTURING -> {
                if (order.getOrderType() != OrderType.PRESCRIPTION) {
                    throw new AppException(ErrorCode.VALIDATION_ERROR, "Only prescription orders can move to manufacturing");
                }
                if (currentStatus != OrderStatus.MANUFACTURING && currentStatus != OrderStatus.PACKING) {
                    throw new AppException(ErrorCode.VALIDATION_ERROR, "Prescription order must be in manufacturing flow");
                }
            }
            case PACKING -> {
                if (currentStatus != OrderStatus.WAITING_FOR_STOCK
                        && currentStatus != OrderStatus.MANUFACTURING
                        && currentStatus != OrderStatus.PACKING) {
                    throw new AppException(ErrorCode.VALIDATION_ERROR, "Order is not ready for packing");
                }
            }
            case READY_TO_SHIP -> {
                if (currentStatus != OrderStatus.PACKING && currentStatus != OrderStatus.READY_TO_SHIP) {
                    throw new AppException(ErrorCode.VALIDATION_ERROR, "Order must be packed before it can be marked ready to ship");
                }
                if (isBlank(order.getLogisticsProvider()) || isBlank(order.getTrackingNumber())) {
                    throw new AppException(ErrorCode.VALIDATION_ERROR, "Logistics provider and tracking number are required before ready to ship");
                }
            }
            case SHIPPING -> {
                if (currentStatus != OrderStatus.READY_TO_SHIP && currentStatus != OrderStatus.SHIPPING) {
                    throw new AppException(ErrorCode.VALIDATION_ERROR, "Order must be ready to ship before shipping");
                }
            }
            case COMPLETED -> {
                if (currentStatus != OrderStatus.SHIPPING) {
                    throw new AppException(ErrorCode.VALIDATION_ERROR, "Only shipping orders can be completed");
                }
            }
            default -> throw new AppException(ErrorCode.VALIDATION_ERROR, "Unsupported operation status");
        }
    }

    private void validateOrderIsActive(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Canceled order cannot be updated");
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Completed order cannot be updated");
        }
        if (!isOperationOrder(order) && order.getStatus() != OrderStatus.PACKING && order.getStatus() != OrderStatus.READY_TO_SHIP) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Order is not in operations flow");
        }
    }

    private void applyStatus(Order order, OrderStatus targetStatus, String actorEmail, String note) {
        order.setStatus(targetStatus);
        order.setUpdatedAt(LocalDateTime.now());
        saveHistory(order, actorEmail, note);
    }

    private void saveHistory(Order order, String actorEmail, String note) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(order.getStatus());
        history.setNote(buildHistoryNote(actorEmail, note));
        history.setCreatedAt(order.getUpdatedAt() != null ? order.getUpdatedAt() : LocalDateTime.now());
        orderStatusHistoryRepository.save(history);
    }

    private String buildHistoryNote(String actorEmail, String note) {
        List<String> parts = new ArrayList<>();
        if (!isBlank(actorEmail)) {
            parts.add("Updated by " + actorEmail.trim());
        }
        if (!isBlank(note)) {
            parts.add(note.trim());
        }
        return parts.isEmpty() ? null : String.join(" | ", parts);
    }

    private String buildLogisticsNote(AssignLogisticsRequest request) {
        List<String> parts = new ArrayList<>();
        if (!isBlank(request.getCarrier())) {
            parts.add("Carrier: " + request.getCarrier().trim());
        }
        if (!isBlank(request.getShippingMethod())) {
            parts.add("Method: " + request.getShippingMethod().trim());
        }
        if (request.getEstimatedDeliveryDate() != null) {
            parts.add("ETA: " + request.getEstimatedDeliveryDate());
        }
        return parts.isEmpty() ? "Logistics assigned" : String.join(" | ", parts);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private List<Long> extractOrderIds(List<Order> orders) {
        return orders.stream()
                .filter(Objects::nonNull)
                .map(Order::getId)
                .filter(Objects::nonNull)
                .toList();
    }

    private Map<Long, Prescription> getPrescriptionsByOrderId(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Prescription> prescriptionsByOrderId = new HashMap<>();
        for (Prescription prescription : prescriptionRepository.findByOrderIdIn(orderIds)) {
            if (prescription.getOrder() != null && prescription.getOrder().getId() != null) {
                prescriptionsByOrderId.put(prescription.getOrder().getId(), prescription);
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

    private OrderResponse toOrderResponse(Order order, Prescription prescription, Payment payment) {
        BigDecimal subtotal = defaultAmount(order.getTotalAmount())
                .subtract(defaultAmount(order.getShippingFee()))
                .add(defaultAmount(order.getDiscountAmount()));
        BigDecimal lensPrice = prescription != null ? defaultAmount(prescription.getLensPriceSnapshot()) : BigDecimal.ZERO;
        BigDecimal itemSubtotal = subtotal.subtract(lensPrice).max(BigDecimal.ZERO);

        return OrderResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .paymentMethod(payment != null ? payment.getMethod() : null)
                .customerEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .status(order.getStatus())
                .orderStatus(order.getStatus())
                .statusLabel(getStatusLabel(order.getStatus()))
                .customerTab(getCustomerTab(order.getStatus()))
                .orderType(order.getOrderType())
                .prescriptionOption(order.getPrescriptionOption())
                .prescriptionReviewStatus(prescription != null ? resolveEffectiveReviewStatus(prescription) : null)
                .totalAmount(order.getTotalAmount())
                .subtotal(subtotal.max(BigDecimal.ZERO))
                .shippingFee(defaultAmount(order.getShippingFee()))
                .discountAmount(defaultAmount(order.getDiscountAmount()))
                .finalAmount(defaultAmount(order.getTotalAmount()).add(defaultAmount(order.getShippingFee())).subtract(defaultAmount(order.getDiscountAmount())))
                .paymentStatus(payment != null ? payment.getStatus() : PaymentStatus.UNPAID)
                .shippingAddress(order.getShippingAddress())
                .city(order.getCity())
                .district(order.getDistrict())
                .ward(order.getWard())
                .addressDetail(order.getAddressDetail())
                .phoneNumber(order.getPhoneNumber())
                .receiverName(order.getReceiverName())
                .note(order.getNote())
                .logisticsProvider(order.getLogisticsProvider())
                .shippingMethod(order.getShippingMethod())
                .trackingNumber(order.getTrackingNumber())
                .estimatedDeliveryDate(order.getEstimatedDeliveryDate())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .payment(payment != null ? mapPaymentSummary(payment) : null)
                .lens(mapLensSummaryResponse(prescription))
                .prescription(mapPrescriptionResponse(prescription))
                .priceSummary(PriceSummaryResponse.builder()
                        .itemsSubtotal(itemSubtotal)
                        .lensPrice(lensPrice)
                        .shippingFee(defaultAmount(order.getShippingFee()))
                        .total(defaultAmount(order.getTotalAmount()).add(defaultAmount(order.getShippingFee())).subtract(defaultAmount(order.getDiscountAmount())))
                        .build())
                .statusHistory(mapStatusHistory(order.getId()))
                .items(mapOrderItems(order.getItems()))
                .build();
    }

    private List<OrderStatusHistoryResponse> mapStatusHistory(Long orderId) {
        return orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAscIdAsc(orderId).stream()
                .map(history -> OrderStatusHistoryResponse.builder()
                        .id(history.getId())
                        .status(history.getStatus())
                        .statusLabel(getStatusLabel(history.getStatus()))
                        .note(history.getNote())
                        .createdAt(history.getCreatedAt())
                        .build())
                .toList();
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
                        .thumbnailUrl(resolveThumbnailUrl(item))
                        .build())
                .toList();
    }

    private PrescriptionResponse mapPrescriptionResponse(Prescription prescription) {
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
                .reviewStatus(resolveEffectiveReviewStatus(prescription))
                .reviewNote(prescription.getReviewNote())
                .build();
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

    private PrescriptionReviewStatus resolveEffectiveReviewStatus(Prescription prescription) {
        if (prescription == null) {
            return null;
        }

        if (prescription.getReviewStatus() != null) {
            return prescription.getReviewStatus();
        }

        if (prescription.getVerifiedBy() != null || prescription.getVerifiedAt() != null) {
            return PrescriptionReviewStatus.APPROVED;
        }

        return PrescriptionReviewStatus.PENDING;
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

        return item.getPrice().divide(BigDecimal.valueOf(item.getQuantity()), 2, RoundingMode.HALF_UP);
    }

    private String resolveThumbnailUrl(OrderItem item) {
        if (item == null
                || item.getProductVariant() == null
                || item.getProductVariant().getProduct() == null
                || item.getProductVariant().getProduct().getImages() == null
                || item.getProductVariant().getProduct().getImages().isEmpty()) {
            return null;
        }

        return item.getProductVariant().getProduct().getImages().stream()
                .filter(Objects::nonNull)
                .map(ProductImage::getImageUrl)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String getStatusLabel(OrderStatus status) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case PENDING_PAYMENT -> "Pending payment";
            case PENDING_VERIFICATION -> "Pending verification";
            case WAITING_FOR_STOCK -> "Waiting for stock";
            case MANUFACTURING -> "Manufacturing";
            case PACKING -> "Packing";
            case READY_TO_SHIP -> "Ready to ship";
            case SHIPPING -> "Shipping";
            case COMPLETED -> "Completed";
            case CANCELLED -> "Canceled";
        };
    }

    private String getCustomerTab(OrderStatus status) {
        if (status == null) {
            return "tat-ca";
        }

        return switch (status) {
            case PENDING_VERIFICATION, MANUFACTURING, PACKING, READY_TO_SHIP -> "cho-gia-cong";
            case SHIPPING -> "van-chuyen";
            case PENDING_PAYMENT, WAITING_FOR_STOCK -> "cho-giao-hang";
            case COMPLETED -> "hoan-thanh";
            case CANCELLED -> "da-huy";
        };
    }
}
