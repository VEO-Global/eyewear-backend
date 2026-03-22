package com.veo.backend.service.impl;

import com.veo.backend.dto.request.CartItemRequest;
import com.veo.backend.dto.response.CartItemResponse;
import com.veo.backend.dto.response.CartResponse;
import com.veo.backend.entity.Cart;
import com.veo.backend.entity.CartItem;
import com.veo.backend.entity.LensProduct;
import com.veo.backend.entity.ProductVariant;
import com.veo.backend.entity.User;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.CartItemRepository;
import com.veo.backend.repository.CartRepository;
import com.veo.backend.repository.LensProductRepository;
import com.veo.backend.repository.ProductVariantRepository;
import com.veo.backend.repository.UserRepository;
import com.veo.backend.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository variantRepository;
    private final LensProductRepository lensProductRepository;

    @Override
    public CartResponse getMyCart(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        Cart cart = cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUser(user);
            return cartRepository.save(newCart);
        });

        List<CartItemResponse> items = cart.getItems() == null ? List.of() : cart.getItems().stream().map(this::mapToItemResponse).collect(Collectors.toList());

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(user.getId())
                .items(items)
                .build();
    }

    @Override
    @Transactional
    public CartItemResponse addToCart(String email, CartItemRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        if (request.getProductVariantId() == null || request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Product variant and positive quantity are required");
        }

        ProductVariant variant = variantRepository.findById(request.getProductVariantId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND, "Product variant not found"));

        if (variant.getStockQuantity() < request.getQuantity()) {
            throw new AppException(ErrorCode.PRODUCT_VARIANT_OUT_STOCK, "Product variant out of stock");
        }

        final LensProduct lensProduct = request.getLensProductId() == null ? null : lensProductRepository.findById(request.getLensProductId())
                .orElseThrow(() -> new AppException(ErrorCode.LENS_PRODUCT_NOT_FOUND, "Lens product not found"));

        Cart cart = cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUser(user);
            return cartRepository.save(newCart);
        });

        if (cart.getItems() == null) {
            cart.setItems(new java.util.ArrayList<>());
        }

        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(i -> i.getProductVariant().getId().equals(variant.getId())
                        && ((i.getLensProduct() == null && lensProduct == null) || (i.getLensProduct() != null && lensProduct != null && i.getLensProduct().getId().equals(lensProduct.getId()))))
                .findFirst();

        CartItem cartItem;
        if (existingItemOpt.isPresent()) {
            cartItem = existingItemOpt.get();
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
            cartItemRepository.save(cartItem);
        } else {
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProductVariant(variant);
            cartItem.setLensProduct(lensProduct);
            cartItem.setQuantity(request.getQuantity());
            cartItem = cartItemRepository.save(cartItem);
        }

        cart.getItems().add(cartItem);
        cartRepository.save(cart);

        return mapToItemResponse(cartItem);
    }

    @Override
    @Transactional
    public void removeCartItem(String email, Long cartItemId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.VALIDATION_ERROR, "Cart not found"));

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new AppException(ErrorCode.VALIDATION_ERROR, "Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Cart item does not belong to user");
        }

        if (cart.getItems() != null) {
            cart.getItems().removeIf(i -> i.getId().equals(cartItemId));
            cartRepository.save(cart);
        }

        cartItemRepository.delete(item);
    }

    private CartItemResponse mapToItemResponse(CartItem item) {
        return CartItemResponse.builder()
                .itemId(item.getId())
                .productVariantId(item.getProductVariant().getId())
                .lensProductId(item.getLensProduct() == null ? null : item.getLensProduct().getId())
                .quantity(item.getQuantity())
                .productName(item.getProductVariant().getProduct().getName())
                .variantSku(item.getProductVariant().getSku())
                .color(item.getProductVariant().getColor())
                .size(item.getProductVariant().getSize())
                .lensName(item.getLensProduct() == null ? null : item.getLensProduct().getName())
                .build();
    }
}
