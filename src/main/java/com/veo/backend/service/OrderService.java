package com.veo.backend.service;

import com.veo.backend.dto.request.OrderCreateRequest;
import com.veo.backend.dto.response.OrderCreateResponse;
import com.veo.backend.dto.response.OrderResponse;
import com.veo.backend.dto.response.PagedResponse;
import com.veo.backend.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderCreateResponse createOrder(String email, OrderCreateRequest request);

    PagedResponse<OrderResponse> getMyOrders(String email, String tab, String status, int page, int size);

    OrderResponse getMyOrderDetail(String email, Long orderId);

    OrderResponse getByOrderId(Long orderId);

    List<OrderResponse> getByUserId(Long userId);

    List<OrderResponse> getMyOrders(String email);
}
