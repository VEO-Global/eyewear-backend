package com.veo.backend.service.impl;

import com.veo.backend.dto.response.FavoriteStatusResponse;
import com.veo.backend.dto.response.ProductResponse;
import com.veo.backend.entity.FavoriteProduct;
import com.veo.backend.entity.Product;
import com.veo.backend.entity.User;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.FavoriteProductRepository;
import com.veo.backend.repository.ProductRepository;
import com.veo.backend.repository.UserRepository;
import com.veo.backend.service.FavoriteProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteProductServiceImpl implements FavoriteProductService {
    private final FavoriteProductRepository favoriteProductRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductResponseMapper productResponseMapper;

    @Override
    @Transactional
    public ProductResponse addFavorite(Long userId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found"));

        FavoriteProduct existingFavorite = favoriteProductRepository.findByUserIdAndProductId(userId, productId)
                .orElse(null);
        if (existingFavorite != null) {
            return productResponseMapper.map(existingFavorite.getProduct());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        favoriteProductRepository.save(FavoriteProduct.builder()
                .user(user)
                .product(product)
                .build());

        return productResponseMapper.map(product);
    }

    @Override
    @Transactional
    public void removeFavorite(Long userId, Long productId) {
        favoriteProductRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getFavoritesByUser(Long userId) {
        ensureUserExists(userId);
        return favoriteProductRepository.findAllByUserIdWithProduct(userId)
                .stream()
                .map(FavoriteProduct::getProduct)
                .map(productResponseMapper::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FavoriteStatusResponse getFavoriteStatus(Long userId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found"));

        ensureUserExists(userId);

        return FavoriteStatusResponse.builder()
                .productId(product.getId())
                .isFavorite(favoriteProductRepository.existsByUserIdAndProductId(userId, productId))
                .build();
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND, "User not found");
        }
    }
}
