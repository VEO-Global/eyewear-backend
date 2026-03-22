package com.veo.backend.controller;

import org.springframework.security.core.Authentication;
import com.veo.backend.dto.request.OrderCreateRequest;
import com.veo.backend.dto.response.OrderCreateResponse;
import com.veo.backend.dto.response.OrderResponse;
import com.veo.backend.dto.response.PagedResponse;
import com.veo.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping("/my")
    public ResponseEntity<PagedResponse<OrderResponse>> getMyOrders(
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        String email = authentication.getName();

        return ResponseEntity.ok(orderService.getMyOrders(email, tab, status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getMyOrderDetail(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String email = authentication.getName();

        return ResponseEntity.ok(orderService.getMyOrderDetail(email, id));
    }

    @PostMapping
    public ResponseEntity<OrderCreateResponse> createOrder(
            @RequestBody OrderCreateRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();

        OrderCreateResponse response = orderService.createOrder(email, request);

        return ResponseEntity.ok(response);
    }
}
