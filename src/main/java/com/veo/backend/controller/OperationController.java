package com.veo.backend.controller;

import com.veo.backend.dto.response.OrderResponse;
import com.veo.backend.service.OperationOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.veo.backend.dto.response.OrderResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/operaion")
@PreAuthorize("hasRole('OPERATIONS')")
public class OperationController {
    private final OperationOrderService operationOrderService;

    @GetMapping("/orders/manufacturing")
    public ResponseEntity<List<OrderResponse>> getManufacturingOrders() {
        return ResponseEntity.ok(operationOrderService.getManufacturingOrders());
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderResponse> getOrderDetail(@PathVariable Long orderId) {
        return ResponseEntity.ok(operationOrderService.getManufacturingOrderDetail(orderId));
    }

    @PatchMapping("/orders/{orderId}/manufacture")
    public ResponseEntity<OrderResponse> updateToManufacturing(@PathVariable Long orderId){
        return ResponseEntity.ok(operationOrderService.updateToManufacturing(orderId));
    }

    @PatchMapping("/orders/{orderId}/ship")
    public ResponseEntity<OrderResponse> updateToShipping(@PathVariable Long orderId){
        return ResponseEntity.ok(operationOrderService.updateToShipping(orderId));
    }

    @PatchMapping("/orders/{orderId}/complete")
    public ResponseEntity<OrderResponse> updateToComplete(@PathVariable Long orderId){
        return ResponseEntity.ok(operationOrderService.updateToCompleted(orderId));
    }
}
