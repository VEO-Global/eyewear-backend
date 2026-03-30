package com.veo.backend.service.impl;

import com.veo.backend.dto.response.OrderResponse;
import com.veo.backend.entity.Order;
import com.veo.backend.enums.OrderStatus;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.OrderRepository;
import com.veo.backend.service.OperationOrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationOrderServiceImpl implements OperationOrderService {
    private final OrderRepository orderRepository;

    @Override
    public List<OrderResponse> getManufacturingOrders() {
        return orderRepository.findByStatusIn(List.of(OrderStatus.WAITING_FOR_STOCK, OrderStatus.MANUFACTURING))
                .stream()
                .map(this::fromEntity)
                .toList();
    }

    @Override
    public OrderResponse getManufacturingOrderDetail(Long id) {
        return orderRepository.findById(id)
                .map(this::fromEntity)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));
    }

    @Override
    public OrderResponse updateToManufacturing(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));

        if (order.getStatus() != OrderStatus.WAITING_FOR_STOCK) {
            throw new AppException(ErrorCode.PRE_ORDER, "This is pre-order and not have stock");
        }
        order.setStatus(OrderStatus.MANUFACTURING);

        return fromEntity(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse updateToShipping(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));

        if (order.getStatus() != OrderStatus.MANUFACTURING) {
            throw new AppException(ErrorCode.ORDER_NOT_MANUFACTURED, "Order not manufactured");
        }
        order.setStatus(OrderStatus.SHIPPING);

        return fromEntity(orderRepository.save(order));
    }

    @Override
    public OrderResponse updateToCompleted(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));

        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new AppException(ErrorCode.ORDER_NOT_SHIPPED, "Order not shipped");
        }
        order.setStatus(OrderStatus.COMPLETED);

        return fromEntity(orderRepository.save(order));
    }

    public OrderResponse fromEntity(Order order) {
        if (order == null) return null;

        OrderResponse.OrderResponseBuilder builder = OrderResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .orderType(order.getOrderType())
                .customerEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .status(order.getStatus())
                .orderStatus(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .note(order.getNote());

        mapShippingInfo(order, builder);
        mapFinancials(order, builder);

        return builder.build();
    }

    private void mapFinancials(Order order, OrderResponse.OrderResponseBuilder builder) {
        builder.totalAmount(order.getTotalAmount());
    }

    private void mapShippingInfo(Order order, OrderResponse.OrderResponseBuilder builder) {
        builder.receiverName(order.getReceiverName())
                .phoneNumber(order.getPhoneNumber())
                .addressDetail(order.getAddressDetail())
                .ward(order.getWard())
                .district(order.getDistrict())
                .city(order.getCity());
    }
}
