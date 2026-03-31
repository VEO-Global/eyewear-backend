package com.veo.backend.service;

import com.veo.backend.dto.request.AssignLogisticsRequest;
import com.veo.backend.dto.request.OperationReceiveStockRequest;
import com.veo.backend.dto.request.OperationStatusUpdateRequest;
import com.veo.backend.dto.request.OrderTrackingRequest;
import com.veo.backend.dto.response.OperationOrderSummaryResponse;
import com.veo.backend.dto.response.OrderResponse;

import java.util.List;

public interface OperationOrderService {
    List<OrderResponse> getOrders(String orderType, String status, String keyword);

    OperationOrderSummaryResponse getOrderSummary();

    OrderResponse getOrderDetail(Long id);

    OrderResponse updateOrderStatus(Long id, String actorEmail, OperationStatusUpdateRequest request);

    OrderResponse assignLogistics(Long id, String actorEmail, AssignLogisticsRequest request);

    OrderResponse updateTracking(Long id, String actorEmail, OrderTrackingRequest request);

    OrderResponse receivePreOrderStock(Long id, String actorEmail, OperationReceiveStockRequest request);
}
