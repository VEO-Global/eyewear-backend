package com.veo.backend.service.impl;

import com.veo.backend.dto.request.ProductVariantCreateRequest;
import com.veo.backend.dto.request.ProductVariantUpdateRequest;
import com.veo.backend.dto.response.ProductVariantResponse;
import com.veo.backend.entity.Product;
import com.veo.backend.entity.ProductVariant;
import com.veo.backend.repository.ProductRepository;
import com.veo.backend.repository.ProductVariantRepository;
import com.veo.backend.service.ProductService;
import com.veo.backend.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;

    @Override
    public List<ProductVariantResponse> getVariantsByProduct(Long productId) {
        return productVariantRepository.findByProductId(productId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public ProductVariantResponse getVariantById(Long id) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product variant not found"));

        return mapToResponse(variant);
    }

    @Override
    public ProductVariantResponse createVariant(ProductVariantCreateRequest request) {
        if (productVariantRepository.existsBySku(request.getSku())) {
            throw new RuntimeException("Product variant sku already exists");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(request.getSku())
                .color(request.getColor())
                .size(request.getSize())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
                .expectedRestockDate(request.getExpectedRestockDate())
                .isActive(true)
                .build();
        return mapToResponse(productVariantRepository.save(variant));
    }

    @Override
    public ProductVariantResponse updateVariant(Long id, ProductVariantUpdateRequest request) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product variant not found"));

        if (request.getColor() != null) variant.setColor(request.getColor());
        if (request.getSize() != null) variant.setSize(request.getSize());
        if (request.getPrice() != null) variant.setPrice(request.getPrice());
        if (request.getStockQuantity() != null) variant.setStockQuantity(request.getStockQuantity());
        if (request.getExpectedRestockDate() != null) variant.setExpectedRestockDate(request.getExpectedRestockDate());
        if (request.getIsActive() != null) variant.setIsActive(request.getIsActive());

        return mapToResponse(productVariantRepository.save(variant));
    }

    @Override
    public void deleteVariant(Long id) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Variant not found with id: " + id));

        variant.setIsActive(false);
        productVariantRepository.save(variant);
    }

    private ProductVariantResponse mapToResponse(ProductVariant variant) {
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .color(variant.getColor())
                .size(variant.getSize())
                .price(variant.getPrice())
                .stockQuantity(variant.getStockQuantity())
                .expectedRestockDate(variant.getExpectedRestockDate())
                .isActive(variant.getIsActive())
                .build();
    }
}
