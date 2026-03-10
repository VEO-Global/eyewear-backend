package com.veo.backend.controller;

import org.springframework.security.core.Authentication;
import com.veo.backend.dto.request.OrderCreateRequest;
import com.veo.backend.dto.response.OrderCreateResponse;
import com.veo.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

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
