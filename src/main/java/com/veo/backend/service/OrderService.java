package com.veo.backend.service;

import com.veo.backend.dto.request.OrderCreateRequest;
import com.veo.backend.dto.response.OrderCreateResponse;
import com.veo.backend.dto.response.OrderResponse;
import com.veo.backend.dto.response.PagedResponse;

public interface OrderService {
    OrderCreateResponse createOrder(String email, OrderCreateRequest request);

    PagedResponse<OrderResponse> getMyOrders(String email, String tab, String status, int page, int size);

    OrderResponse getMyOrderDetail(String email, Long orderId);
}
