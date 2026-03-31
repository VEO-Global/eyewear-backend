package com.veo.backend.controller;

import com.veo.backend.dto.request.AssignLogisticsRequest;
import com.veo.backend.dto.request.OperationReceiveStockRequest;
import com.veo.backend.dto.request.OperationStatusUpdateRequest;
import com.veo.backend.dto.request.OrderTrackingRequest;
import com.veo.backend.dto.response.OperationOrderSummaryResponse;
import com.veo.backend.dto.response.OrderResponse;
import com.veo.backend.service.OperationOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/operation", "/api/operaion"})
@PreAuthorize("hasRole('OPERATIONS')")
public class OperationController {
    private final OperationOrderService operationOrderService;

    @GetMapping("/orders")
    public List<OrderResponse> getOrders(
            @RequestParam(required = false) String orderType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        return operationOrderService.getOrders(orderType, status, keyword);
    }

    @GetMapping("/orders/summary")
    public OperationOrderSummaryResponse getOrderSummary() {
        return operationOrderService.getOrderSummary();
    }

    @GetMapping("/orders/{orderId}")
    public OrderResponse getOrderDetail(@PathVariable Long orderId) {
        return operationOrderService.getOrderDetail(orderId);
    }

    @PatchMapping("/orders/{orderId}/status")
    public OrderResponse updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OperationStatusUpdateRequest request,
            Authentication authentication
    ) {
        return operationOrderService.updateOrderStatus(orderId, authentication.getName(), request);
    }

    @PatchMapping("/orders/{orderId}/logistics")
    public OrderResponse assignLogistics(
            @PathVariable Long orderId,
            @Valid @RequestBody AssignLogisticsRequest request,
            Authentication authentication
    ) {
        return operationOrderService.assignLogistics(orderId, authentication.getName(), request);
    }

    @PatchMapping("/orders/{orderId}/tracking")
    public OrderResponse updateTracking(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderTrackingRequest request,
            Authentication authentication
    ) {
        return operationOrderService.updateTracking(orderId, authentication.getName(), request);
    }

    @PatchMapping("/orders/{orderId}/receive-stock")
    public OrderResponse receivePreOrderStock(
            @PathVariable Long orderId,
            @Valid @RequestBody OperationReceiveStockRequest request,
            Authentication authentication
    ) {
        return operationOrderService.receivePreOrderStock(orderId, authentication.getName(), request);
    }
}
