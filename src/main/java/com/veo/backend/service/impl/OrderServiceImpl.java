package com.veo.backend.service.impl;

import com.veo.backend.dto.request.OrderCreateRequest;
import com.veo.backend.dto.request.OrderItemRequest;
import com.veo.backend.dto.request.PrescriptionRequest;
import com.veo.backend.dto.response.LensSummaryResponse;
import com.veo.backend.dto.response.OrderCreateResponse;
import com.veo.backend.dto.response.OrderItemResponse;
import com.veo.backend.dto.response.OrderResponse;
import com.veo.backend.dto.response.PagedResponse;
import com.veo.backend.dto.response.PrescriptionResponse;
import com.veo.backend.dto.response.PriceSummaryResponse;
import com.veo.backend.entity.*;
import com.veo.backend.enums.OrderStatus;
import com.veo.backend.enums.OrderType;
import com.veo.backend.enums.PaymentStatus;
import com.veo.backend.enums.ProductCatalogType;
import com.veo.backend.enums.PrescriptionOption;
import com.veo.backend.enums.PrescriptionReviewStatus;
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
import java.util.Objects;
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
    private final SystemConfigRepository systemConfigRepository;

    @Override
    @Transactional
    public OrderCreateResponse createOrder(String email, OrderCreateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        PrescriptionOption prescriptionOption = request.getPrescriptionOption();

        if (prescriptionOption == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Prescription option is required");
        }

        validatePrescriptionRequest(request, prescriptionOption);
        validateShippingRequest(request);

        LensProduct selectedLens = resolveSelectedLens(request, prescriptionOption);

        List<OrderItem>  orderItems = new ArrayList<>();
        BigDecimal itemsSubtotal = BigDecimal.ZERO;
        boolean hasPreorderItem = false;

        for (OrderItemRequest itemRequest : request.getItems()){
            Long variantId = itemRequest.resolveVariantId();
            ProductVariant variant = variantRepository.findById(variantId)
                    .orElseThrow(()-> new AppException(ErrorCode.VALIDATION_ERROR, "Variant not found"));

            Integer quantity = itemRequest.getQuantity();

            if (isPreorderVariant(variant)) {
                hasPreorderItem = true;
            }

            if (!isPreorderVariant(variant)) {
                if (variant.getStockQuantity() < quantity) {
                    throw new AppException(ErrorCode.PRODUCT_VARIANT_OUT_STOCK, "Product variant out of stock");
                }

                variant.setStockQuantity(variant.getStockQuantity() - quantity);
                variantRepository.save(variant);
            }

            BigDecimal itemTotal = defaultAmount(variant.getPrice()).multiply(BigDecimal.valueOf(quantity));
            itemsSubtotal = itemsSubtotal.add(itemTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setProductVariant(variant);
            orderItem.setLensProduct(null);
            orderItem.setQuantity(quantity);
            orderItem.setPrice(itemTotal);
            orderItems.add(orderItem);
        }

        OrderType orderType = hasPreorderItem
                ? OrderType.PRE_ORDER
                : prescriptionOption == PrescriptionOption.WITH_PRESCRIPTION
                ? OrderType.PRESCRIPTION
                : OrderType.NORMAL;

        BigDecimal lensPrice = selectedLens != null ? defaultAmount(selectedLens.getPrice()) : BigDecimal.ZERO;
        BigDecimal shippingFee = resolveShippingFee();
        BigDecimal subtotalAmount = itemsSubtotal.add(lensPrice);
        BigDecimal finalAmount = subtotalAmount.add(shippingFee);

        Order order = new Order();
        order.setUser(user);
        order.setOrderType(orderType);
        order.setPrescriptionOption(prescriptionOption);
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        String builtAddress = buildNormalizedAddress(
                resolveAddressDetail(request),
                resolveWard(request),
                resolveDistrict(request),
                resolveProvince(request)
        );

        order.setShippingAddress(builtAddress);
        order.setProvince(resolveProvince(request));
        order.setDistrict(resolveDistrict(request));
        order.setWard(resolveWard(request));
        order.setAddressDetail(resolveAddressDetail(request));
        order.setPhoneNumber(request.getPhoneNumber());
        order.setReceiverName(request.getReceiverName());
        order.setNote(request.getNote());
        order.setTotalAmount(subtotalAmount);
        order.setShippingFee(shippingFee);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setCreatedAt(LocalDateTime.now());
        order.setItems(orderItems);

        orderItems.forEach(item -> item.setOrder(order));
        orderRepository.save(order);
        order.setOrderCode("ORD-" + order.getId());
        orderRepository.save(order);
        saveOrUpdateDefaultUserAddress(user, builtAddress, request);

        Prescription savedPrescription = null;
        if (prescriptionOption == PrescriptionOption.WITH_PRESCRIPTION) {
            PrescriptionRequest p = request.getPrescription();
            Prescription prescription = new Prescription();
            prescription.setOrder(order);
            prescription.setLensProduct(selectedLens);
            prescription.setLensNameSnapshot(selectedLens != null ? selectedLens.getName() : null);
            prescription.setLensPriceSnapshot(selectedLens != null ? selectedLens.getPrice() : null);
            prescription.setLensDescriptionSnapshot(selectedLens != null ? selectedLens.getDescription() : null);
            prescription.setPrescriptionImageUrl(p.getPrescriptionImageUrl());
            prescription.setSphereOd(p.getSphereOd());
            prescription.setSphereOs(p.getSphereOs());
            prescription.setCylinderOd(p.getCylinderOd());
            prescription.setCylinderOs(p.getCylinderOs());
            prescription.setAxisOd(p.getAxisOd());
            prescription.setAxisOs(p.getAxisOs());
            prescription.setPd(p.getPd());
            prescription.setReviewStatus(PrescriptionReviewStatus.PENDING);
            prescription.setCreatedAt(LocalDateTime.now());
            savedPrescription = prescriptionRepository.save(prescription);
        }

        return OrderCreateResponse.builder()
                .orderId(order.getId())
                .totalAmount(subtotalAmount)
                .orderCode(order.getOrderCode())
                .shippingFee(shippingFee)
                .discountAmount(defaultAmount(order.getDiscountAmount()))
                .finalAmount(finalAmount)
                .orderStatus(order.getStatus())
                .paymentStatus(PaymentStatus.UNPAID)
                .prescriptionOption(prescriptionOption)
                .prescriptionReviewStatus(savedPrescription != null ? savedPrescription.getReviewStatus() : null)
                .items(mapOrderItems(order.getItems()))
                .lens(mapLensSummaryResponse(savedPrescription))
                .prescription(mapPrescriptionResponse(savedPrescription))
                .priceSummary(PriceSummaryResponse.builder()
                        .itemsSubtotal(itemsSubtotal)
                        .lensPrice(lensPrice)
                        .shippingFee(shippingFee)
                        .total(finalAmount)
                        .build())
                .createdAt(order.getCreatedAt())
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
        userAddress.setAddressDetail(trimToNull(resolveAddressDetail(request)));
        userAddress.setWard(trimToNull(resolveWard(request)));
        userAddress.setDistrict(trimToNull(resolveDistrict(request)));
        userAddress.setCity(defaultIfNull(trimToNull(resolveProvince(request)), ""));
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
        Prescription prescription = prescriptionRepository.findByOrderId(order.getId()).orElse(null);
        BigDecimal subtotal = defaultAmount(order.getTotalAmount()).subtract(defaultAmount(order.getShippingFee())).add(defaultAmount(order.getDiscountAmount()));
        BigDecimal lensPrice = prescription != null ? defaultAmount(prescription.getLensPriceSnapshot()) : BigDecimal.ZERO;
        BigDecimal itemSubtotal = subtotal.subtract(lensPrice).max(BigDecimal.ZERO);

        return OrderResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .customerEmail(order.getUser().getEmail())
                .status(order.getStatus())
                .orderStatus(order.getStatus())
                .statusLabel(getStatusLabel(order.getStatus()))
                .customerTab(getCustomerTab(order.getStatus()))
                .orderType(order.getOrderType())
                .totalAmount(order.getTotalAmount())
                .subtotal(subtotal.max(BigDecimal.ZERO))
                .finalAmount(defaultAmount(order.getTotalAmount()).add(defaultAmount(order.getShippingFee())).subtract(defaultAmount(order.getDiscountAmount())))
                .prescriptionOption(order.getPrescriptionOption() != null ? order.getPrescriptionOption()
                        : (prescription != null ? PrescriptionOption.WITH_PRESCRIPTION : PrescriptionOption.WITHOUT_PRESCRIPTION))
                .prescriptionReviewStatus(prescription != null ? prescription.getReviewStatus() : null)
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
                .lens(mapLensSummaryResponse(prescription))
                .prescription(mapPrescriptionResponse(prescription))
                .priceSummary(PriceSummaryResponse.builder()
                        .itemsSubtotal(itemSubtotal)
                        .lensPrice(lensPrice)
                        .shippingFee(defaultAmount(order.getShippingFee()))
                        .total(defaultAmount(order.getTotalAmount()).add(defaultAmount(order.getShippingFee())).subtract(defaultAmount(order.getDiscountAmount())))
                        .build())
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
                .collect(Collectors.toList());
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
                .reviewStatus(prescription.getReviewStatus())
                .reviewNote(prescription.getReviewNote())
                .build();
    }

    private LensSummaryResponse mapLensSummaryResponse(Prescription prescription) {
        if (prescription == null) {
            return null;
        }

        return LensSummaryResponse.builder()
                .id(prescription.getLensProduct() != null ? prescription.getLensProduct().getId() : null)
                .name(prescription.getLensNameSnapshot())
                .price(prescription.getLensPriceSnapshot())
                .description(prescription.getLensDescriptionSnapshot())
                .build();
    }

    private BigDecimal resolveUnitPrice(OrderItem item) {
        if (item == null || item.getQuantity() == null || item.getQuantity() <= 0 || item.getPrice() == null) {
            return null;
        }

        return item.getPrice().divide(BigDecimal.valueOf(item.getQuantity()), 2, java.math.RoundingMode.HALF_UP);
    }

    private String resolveThumbnailUrl(OrderItem item) {
        if (item == null || item.getProductVariant() == null || item.getProductVariant().getProduct() == null) {
            return null;
        }

        return null;
    }

    private void validatePrescriptionRequest(OrderCreateRequest request, PrescriptionOption prescriptionOption) {
        boolean hasPrescription = request.getPrescription() != null;
        boolean hasLensSelection = request.getLensProductId() != null || (request.getItems() != null && request.getItems().stream()
                .anyMatch(item -> item.getLensProductId() != null));

        if (prescriptionOption == PrescriptionOption.WITHOUT_PRESCRIPTION) {
            if (hasPrescription || hasLensSelection) {
                throw new AppException(ErrorCode.VALIDATION_ERROR,
                        "Prescription and lens selection are not allowed when prescription option is WITHOUT_PRESCRIPTION");
            }
            return;
        }

        if (!hasPrescription) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Prescription is required when prescription option is WITH_PRESCRIPTION");
        }

        PrescriptionRequest prescription = request.getPrescription();
        boolean hasImage = prescription.getPrescriptionImageUrl() != null && !prescription.getPrescriptionImageUrl().isBlank();
        boolean hasManualInput = prescription.getSphereOd() != null
                || prescription.getSphereOs() != null
                || prescription.getCylinderOd() != null
                || prescription.getCylinderOs() != null
                || prescription.getAxisOd() != null
                || prescription.getAxisOs() != null
                || prescription.getPd() != null;

        if (!hasImage && !hasManualInput) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Prescription image or manual prescription values are required");
        }

        if ((prescription.getCylinderOd() != null && prescription.getAxisOd() == null)
                || (prescription.getCylinderOs() != null && prescription.getAxisOs() == null)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Axis is required when cylinder value is provided");
        }

        if (prescription.getPd() == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "PD is required when prescription option is WITH_PRESCRIPTION");
        }

        if (prescription.getPd().compareTo(BigDecimal.valueOf(40)) < 0
                || prescription.getPd().compareTo(BigDecimal.valueOf(80)) > 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "PD must be between 40 and 80");
        }
    }

    private void validateShippingRequest(OrderCreateRequest request) {
        if (resolveAddressDetail(request) == null || resolveAddressDetail(request).isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Address detail is required");
        }
    }

    private LensProduct resolveSelectedLens(OrderCreateRequest request, PrescriptionOption prescriptionOption) {
        Long lensProductId = request.getLensProductId();
        if (lensProductId == null && request.getItems() != null) {
            lensProductId = request.getItems().stream()
                    .map(OrderItemRequest::getLensProductId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }

        if (prescriptionOption == PrescriptionOption.WITH_PRESCRIPTION && lensProductId == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Lens product is required when prescription option is WITH_PRESCRIPTION");
        }

        if (lensProductId == null) {
            return null;
        }

        LensProduct lensProduct = lensProductRepository.findById(lensProductId)
                .orElseThrow(() -> new AppException(ErrorCode.LENS_PRODUCT_NOT_FOUND, "Lens product not found"));

        if (!Boolean.TRUE.equals(lensProduct.getIsActive())) {
            throw new AppException(ErrorCode.LENS_PRODUCT_NOT_VALID, "Lens product not active");
        }

        return lensProduct;
    }

    private BigDecimal resolveShippingFee() {
        return systemConfigRepository.findByConfigKey("shipping.base_fee")
                .map(SystemConfig::getConfigValue)
                .map(value -> {
                    try {
                        return new BigDecimal(value);
                    } catch (NumberFormatException ex) {
                        return BigDecimal.valueOf(30000);
                    }
                })
                .orElse(BigDecimal.valueOf(30000));
    }

    private String resolveProvince(OrderCreateRequest request) {
        if (request.getShippingAddress() != null && request.getShippingAddress().getProvinceName() != null) {
            return request.getShippingAddress().getProvinceName();
        }
        return request.getProvince();
    }

    private String resolveDistrict(OrderCreateRequest request) {
        if (request.getShippingAddress() != null && request.getShippingAddress().getDistrictName() != null) {
            return request.getShippingAddress().getDistrictName();
        }
        return request.getDistrict();
    }

    private String resolveWard(OrderCreateRequest request) {
        if (request.getShippingAddress() != null && request.getShippingAddress().getWardName() != null) {
            return request.getShippingAddress().getWardName();
        }
        return request.getWard();
    }

    private String resolveAddressDetail(OrderCreateRequest request) {
        if (request.getShippingAddress() != null && request.getShippingAddress().getAddressDetail() != null) {
            return request.getShippingAddress().getAddressDetail();
        }
        return request.getAddressDetail();
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

    private boolean isPreorderVariant(ProductVariant variant) {
        return variant.getProduct() != null
                && variant.getProduct().getCatalogType() == ProductCatalogType.NEW;
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
