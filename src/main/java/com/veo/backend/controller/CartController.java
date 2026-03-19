package com.veo.backend.controller;

import com.veo.backend.dto.request.CartItemRequest;
import com.veo.backend.dto.response.CartItemResponse;
import com.veo.backend.dto.response.CartResponse;
import com.veo.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getMyCart(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(cartService.getMyCart(email));
    }

    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addToCart(
            Authentication authentication,
            @RequestBody CartItemRequest request
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(cartService.addToCart(email, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeCartItem(
            Authentication authentication,
            @PathVariable Long itemId
    ) {
        String email = authentication.getName();
        cartService.removeCartItem(email, itemId);
        return ResponseEntity.noContent().build();
    }
}
