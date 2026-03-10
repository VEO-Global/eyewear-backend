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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository variantRepository;
    private final LensProductRepository lensProductRepository;
    private final UserRepository userRepository;
    private final PrescriptionRepository prescriptionRepository;

    @Override
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
        order.setShippingAddress(request.getShippingAddress());
        order.setPhoneNumber(request.getPhoneNumber());
        order.setReceiverName(request.getReceiverName());
        order.setNote(request.getNote());
        order.setTotalAmount(totalAmount);
        order.setCreatedAt(LocalDateTime.now());
        order.setItems(orderItems);

        orderItems.forEach(item -> item.setOrder(order));
        orderRepository.save(order);

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
}
