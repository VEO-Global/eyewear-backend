package com.veo.backend.service;

import com.veo.backend.dto.request.CartItemRequest;
import com.veo.backend.dto.response.CartItemResponse;
import com.veo.backend.dto.response.CartResponse;

public interface CartService {
    CartResponse getMyCart(String email);

    CartItemResponse addToCart(String email, CartItemRequest request);

    void removeCartItem(String email, Long cartItemId);
}
