package com.veo.backend.service.impl;

import com.veo.backend.dto.request.OrderCreateRequest;
import com.veo.backend.dto.request.OrderItemRequest;
import com.veo.backend.dto.request.PrescriptionRequest;
import com.veo.backend.dto.response.OrderCreateResponse;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
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
}
