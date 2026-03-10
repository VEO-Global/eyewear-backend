package com.veo.backend.service;

import com.veo.backend.dto.request.OrderCreateRequest;
import com.veo.backend.dto.response.OrderCreateResponse;

public interface OrderService {
    OrderCreateResponse createOrder(String email, OrderCreateRequest request);
}
