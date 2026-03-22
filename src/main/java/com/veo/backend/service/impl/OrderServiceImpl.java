package com.veo.backend.service.impl;

import com.veo.backend.dto.request.OrderCreateRequest;
import com.veo.backend.dto.request.OrderItemRequest;
import com.veo.backend.dto.request.PrescriptionRequest;
import com.veo.backend.dto.response.OrderCreateResponse;
import com.veo.backend.dto.response.OrderItemResponse;
import com.veo.backend.dto.response.OrderResponse;
import com.veo.backend.dto.response.PagedResponse;
import com.veo.backend.entity.*;
import com.veo.backend.enums.OrderStatus;
import com.veo.backend.enums.OrderType;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.*;
import com.veo.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final LensProductRepository lensProductRepository;
    private final UserRepository userRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final UserAddressRepository userAddressRepository;

    @Override
    @Transactional
    public OrderCreateResponse createOrder(String email, OrderCreateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        OrderType orderType = request.getOrderType();

        if (orderType == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Order type is required");
        }

        List<OrderItem>  orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()){
            ProductVariant variant = variantRepository.findById(itemRequest.getProductVariantId())
                    .orElseThrow(()-> new AppException(ErrorCode.VALIDATION_ERROR, "Variant not found"));

            Integer quantity = itemRequest.getQuantity();

            LensProduct lensProduct = null;

            if (itemRequest.getLensProductId() != null) {
                lensProduct = lensProductRepository.findById(itemRequest.getLensProductId())
                        .orElseThrow(()-> new AppException(ErrorCode.VALIDATION_ERROR, "LensProduct not found"));
            }

            if (orderType != OrderType.PRE_ORDER) {
                if (variant.getStockQuantity() < quantity) {
                    throw new AppException(ErrorCode.PRODUCT_VARIANT_OUT_STOCK, "Product variant out of stock");
                }

                variant.setStockQuantity(variant.getStockQuantity() - quantity);
                variantRepository.save(variant);
            }

            BigDecimal itemPrice = variant.getPrice();

            if (lensProduct != null) {
                itemPrice = itemPrice.add(lensProduct.getPrice());
            }

            BigDecimal itemTotal = itemPrice.multiply(BigDecimal.valueOf(quantity));

            totalAmount = totalAmount.add(itemTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setProductVariant(variant);
            orderItem.setLensProduct(lensProduct);
            orderItem.setQuantity(quantity);
            orderItem.setPrice(itemTotal);
            orderItems.add(orderItem);
        }

        Order order = new Order();
        order.setUser(user);
        order.setOrderType(orderType);
        order.setStatus(
                orderType == OrderType.PRESCRIPTION
                        ? OrderStatus.PENDING_VERIFICATION
                        : OrderStatus.PENDING_PAYMENT
        );

        // Build normalized shipping address with duplicate suppression
        String builtAddress = buildNormalizedAddress(
                request.getAddressDetail(),
                request.getWard(),
                request.getDistrict(),
                request.getProvince()
        );

        order.setShippingAddress(builtAddress);
        order.setProvince(request.getProvince());
        order.setDistrict(request.getDistrict());
        order.setWard(request.getWard());
        order.setAddressDetail(request.getAddressDetail());
        order.setPhoneNumber(request.getPhoneNumber());
        order.setReceiverName(request.getReceiverName());
        order.setNote(request.getNote());
        order.setTotalAmount(totalAmount);
        order.setCreatedAt(LocalDateTime.now());
        order.setItems(orderItems);

        orderItems.forEach(item -> item.setOrder(order));
        orderRepository.save(order);
        saveOrUpdateDefaultUserAddress(user, builtAddress, request);

        if (orderType == OrderType.PRESCRIPTION) {
            PrescriptionRequest p = request.getPrescription();

            if (p == null) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Prescription not allowed");
            }

            Prescription prescription = new Prescription();
            prescription.setOrder(order);
            prescription.setPrescriptionImageUrl(p.getPrescriptionImageUrl());
            prescription.setSphereOd(p.getSphereOd());
            prescription.setSphereOs(p.getSphereOs());
            prescription.setCylinderOd(p.getCylinderOd());
            prescription.setCylinderOs(p.getCylinderOs());
            prescription.setAxisOd(p.getAxisOd());
            prescription.setAxisOs(p.getAxisOs());
            prescription.setPd(p.getPd());
            prescriptionRepository.save(prescription);
        }

        return OrderCreateResponse.builder()
                .orderId(order.getId())
                .totalAmount(totalAmount)
                .message("Create order successfully")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getMyOrders(String email, String tab, String status, int page, int size) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        if (page < 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Page must be greater than or equal to 0");
        }

        if (size <= 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Size must be greater than 0");
        }

        List<OrderStatus> statuses = resolveStatuses(tab, status);
        List<Order> orders = statuses == null
                ? orderRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                : statuses.isEmpty()
                ? Collections.emptyList()
                : orderRepository.findAllByUserIdAndStatusInOrderByCreatedAtDesc(user.getId(), statuses);

        List<OrderResponse> mappedOrders = orders.stream()
                .map(this::mapOrderResponse)
                .toList();

        int fromIndex = Math.min(page * size, mappedOrders.size());
        int toIndex = Math.min(fromIndex + size, mappedOrders.size());
        List<OrderResponse> content = mappedOrders.subList(fromIndex, toIndex);
        int totalPages = mappedOrders.isEmpty() ? 0 : (int) Math.ceil((double) mappedOrders.size() / size);

        return PagedResponse.<OrderResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(mappedOrders.size())
                .totalPages(totalPages)
                .last(toIndex >= mappedOrders.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrderDetail(String email, Long orderId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN, "You do not have permission to view this order"));

        return mapOrderResponse(order);
    }

    private String buildNormalizedAddress(String addressDetail, String ward, String district, String province) {
        String detail = addressDetail == null ? "" : addressDetail.trim();
        String provinceValue = province == null ? "" : province.trim();
        String districtValue = district == null ? "" : district.trim();
        String wardValue = ward == null ? "" : ward.trim();

        boolean hasProvince = containsNormalizedPart(detail, provinceValue);
        boolean hasDistrict = containsNormalizedPart(detail, districtValue);
        boolean hasWard = containsNormalizedPart(detail, wardValue);

        StringBuilder builder = new StringBuilder();

        if (!detail.isEmpty()) {
            builder.append(detail);
        }

        if (!wardValue.isEmpty() && !hasWard) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(wardValue);
        }

        if (!districtValue.isEmpty() && !hasDistrict) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(districtValue);
        }

        if (!provinceValue.isEmpty() && !hasProvince) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(provinceValue);
        }

        String normalized = builder.toString().trim();
        return normalized.isEmpty() ? detail : normalized;
    }

    private void saveOrUpdateDefaultUserAddress(User user, String normalizedAddress, OrderCreateRequest request) {
        UserAddress userAddress = userAddressRepository
                .findFirstByUserIdOrderByIsDefaultDescIdAsc(user.getId())
                .orElseGet(() -> {
                    UserAddress newAddress = new UserAddress();
                    newAddress.setUser(user);
                    newAddress.setIsDefault(true);
                    return newAddress;
                });

        userAddress.setAddressLine(normalizedAddress);
        userAddress.setAddressDetail(trimToNull(request.getAddressDetail()));
        userAddress.setWard(trimToNull(request.getWard()));
        userAddress.setDistrict(trimToNull(request.getDistrict()));
        userAddress.setCity(defaultIfNull(trimToNull(request.getProvince()), ""));
        userAddress.setIsDefault(true);
        userAddressRepository.save(userAddress);
    }

    private boolean containsNormalizedPart(String base, String part) {
        if (base == null || part == null || part.isBlank()) {
            return false;
        }

        return normalizeForCompare(base).contains(normalizeForCompare(part));
    }

    private String normalizeForCompare(String value) {
        String normalized = Normalizer.normalize(Optional.ofNullable(value).orElse(""), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[,.;\\-_/]+", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultIfNull(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private OrderResponse mapOrderResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .customerEmail(order.getUser().getEmail())
                .status(order.getStatus())
                .statusLabel(getStatusLabel(order.getStatus()))
                .customerTab(getCustomerTab(order.getStatus()))
                .orderType(order.getOrderType())
                .totalAmount(order.getTotalAmount())
                .shippingFee(defaultAmount(order.getShippingFee()))
                .discountAmount(defaultAmount(order.getDiscountAmount()))
                .shippingAddress(order.getShippingAddress())
                .province(order.getProvince())
                .district(order.getDistrict())
                .ward(order.getWard())
                .addressDetail(order.getAddressDetail())
                .phoneNumber(order.getPhoneNumber())
                .receiverName(order.getReceiverName())
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(mapOrderItems(order.getItems()))
                .build();
    }

    private List<OrderItemResponse> mapOrderItems(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return Collections.emptyList();
        }

        return orderItems.stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productVariantId(item.getProductVariant() != null ? item.getProductVariant().getId() : null)
                        .productVariantName(resolveVariantName(item))
                        .lensProductId(item.getLensProduct() != null ? item.getLensProduct().getId() : null)
                        .lensProductName(item.getLensProduct() != null ? item.getLensProduct().getName() : null)
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());
    }

    private String resolveVariantName(OrderItem item) {
        if (item.getProductVariant() == null) {
            return null;
        }

        Product product = item.getProductVariant().getProduct();
        if (product == null) {
            return item.getProductVariant().getSku();
        }

        List<String> parts = new ArrayList<>();
        parts.add(product.getName());

        if (item.getProductVariant().getColor() != null && !item.getProductVariant().getColor().isBlank()) {
            parts.add(item.getProductVariant().getColor());
        }

        if (item.getProductVariant().getSize() != null && !item.getProductVariant().getSize().isBlank()) {
            parts.add("Size " + item.getProductVariant().getSize());
        }

        return String.join(" - ", parts);
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private List<OrderStatus> resolveStatuses(String tab, String status) {
        if (status != null && !status.isBlank()) {
            try {
                return List.of(OrderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Invalid order status");
            }
        }

        if (tab == null || tab.isBlank() || "tat-ca".equalsIgnoreCase(tab.trim())) {
            return null;
        }

        return switch (tab.trim().toLowerCase(Locale.ROOT)) {
            case "cho-gia-cong" -> Arrays.asList(
                    OrderStatus.PENDING_VERIFICATION,
                    OrderStatus.MANUFACTURING
            );
            case "van-chuyen" -> List.of(OrderStatus.SHIPPING);
            case "cho-giao-hang" -> Arrays.asList(
                    OrderStatus.PENDING_PAYMENT,
                    OrderStatus.WAITING_FOR_STOCK
            );
            case "hoan-thanh" -> List.of(OrderStatus.COMPLETED);
            case "da-huy" -> List.of(OrderStatus.CANCELLED);
            case "tra-hang-hoan-tien" -> Collections.emptyList();
            default -> throw new AppException(ErrorCode.VALIDATION_ERROR, "Invalid order tab");
        };
    }

    private String getStatusLabel(OrderStatus status) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case PENDING_PAYMENT -> "Chờ thanh toán";
            case PENDING_VERIFICATION -> "Chờ xác nhận toa";
            case WAITING_FOR_STOCK -> "Chờ có hàng";
            case MANUFACTURING -> "Chờ gia công";
            case SHIPPING -> "Vận chuyển";
            case COMPLETED -> "Hoàn thành";
            case CANCELLED -> "Đã hủy";
        };
    }

    private String getCustomerTab(OrderStatus status) {
        if (status == null) {
            return "tat-ca";
        }

        return switch (status) {
            case PENDING_VERIFICATION, MANUFACTURING -> "cho-gia-cong";
            case SHIPPING -> "van-chuyen";
            case PENDING_PAYMENT, WAITING_FOR_STOCK -> "cho-giao-hang";
            case COMPLETED -> "hoan-thanh";
            case CANCELLED -> "da-huy";
        };
    }
}
