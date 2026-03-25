package com.veo.backend.service;

import com.veo.backend.dto.response.OrderResponse;

import java.util.List;

public interface OperationOrderService {
    List<OrderResponse> getManufacturingOrders();

    OrderResponse getManufacturingOrderDetail(Long id);

    OrderResponse updateToManufacturing(Long id);

    OrderResponse updateToShipping(Long id);

    OrderResponse updateToCompleted(Long id);
}
