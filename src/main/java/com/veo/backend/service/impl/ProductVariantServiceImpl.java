package com.veo.backend.service.impl;

import com.veo.backend.dto.request.BulkVariantStockUpdateRequest;
import com.veo.backend.dto.request.ProductVariantCreateRequest;
import com.veo.backend.dto.request.ProductVariantUpdateRequest;
import com.veo.backend.dto.response.ProductVariantResponse;
import com.veo.backend.entity.Product;
import com.veo.backend.entity.ProductVariant;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.ProductRepository;
import com.veo.backend.repository.ProductVariantRepository;
import com.veo.backend.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;

    @Override
    public List<ProductVariantResponse> getVariantsByProduct(Long productId, Boolean active) {
        List<ProductVariant> variants = active == null
                ? productVariantRepository.findByProductId(productId)
                : productVariantRepository.findByProductIdAndIsActive(productId, active);

        return variants
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<ProductVariantResponse> getVariants(Long productId, Boolean active) {
        List<ProductVariant> variants;
        if (productId != null) {
            variants = active == null
                    ? productVariantRepository.findByProductId(productId)
                    : productVariantRepository.findByProductIdAndIsActive(productId, active);
        } else if (active != null) {
            variants = productVariantRepository.findByIsActive(active);
        } else {
            variants = productVariantRepository.findAll();
        }

        return variants.stream().map(this::mapToResponse).toList();
    }

    @Override
    public ProductVariantResponse getVariantById(Long id) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND, "Product variant not found"));

        return mapToResponse(variant);
    }

    @Override
    @Transactional
    public ProductVariantResponse createVariant(ProductVariantCreateRequest request) {
        String sku = normalizeRequired(request.getSku(), "SKU is required");
        String color = normalizeRequired(request.getColor(), "Color is required");
        String size = normalizeRequired(request.getSize(), "Size is required");

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found"));

        validateSkuForCreate(sku);
        validateVariantCombinationForCreate(product.getId(), color, size);

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(sku)
                .color(color)
                .size(size)
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
                .expectedRestockDate(request.getExpectedRestockDate())
                .isActive(true)
                .build();
        return mapToResponse(productVariantRepository.save(variant));
    }

    @Override
    @Transactional
    public List<ProductVariantResponse> createVariantsBulk(List<ProductVariantCreateRequest> requests) {
        return requests.stream()
                .map(this::createVariant)
                .toList();
    }

    @Override
    @Transactional
    public ProductVariantResponse updateVariant(Long id, ProductVariantUpdateRequest request) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND, "Product variant not found"));

        if (request.getSku() != null) {
            String sku = normalizeRequired(request.getSku(), "SKU must not be blank");
            validateSkuForUpdate(sku, variant.getId());
            variant.setSku(sku);
        }

        String nextColor = variant.getColor();
        if (request.getColor() != null) {
            nextColor = normalizeRequired(request.getColor(), "Color must not be blank");
            variant.setColor(nextColor);
        }

        String nextSize = variant.getSize();
        if (request.getSize() != null) {
            nextSize = normalizeRequired(request.getSize(), "Size must not be blank");
            variant.setSize(nextSize);
        }

        if (request.getColor() != null || request.getSize() != null) {
            validateVariantCombinationForUpdate(variant.getProduct().getId(), nextColor, nextSize, variant.getId());
        }

        if (request.getPrice() != null) variant.setPrice(request.getPrice());
        if (request.getStockQuantity() != null) variant.setStockQuantity(request.getStockQuantity());
        if (request.getExpectedRestockDate() != null) variant.setExpectedRestockDate(request.getExpectedRestockDate());
        if (request.getIsActive() != null) variant.setIsActive(request.getIsActive());

        return mapToResponse(productVariantRepository.save(variant));
    }

    @Override
    @Transactional
    public List<ProductVariantResponse> updateBulkStock(BulkVariantStockUpdateRequest request) {
        return request.getItems().stream()
                .map(item -> {
                    ProductVariant variant = productVariantRepository.findById(item.getVariantId())
                            .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND, "Product variant not found"));
                    variant.setStockQuantity(item.getStockQuantity());
                    return mapToResponse(productVariantRepository.save(variant));
                })
                .toList();
    }

    @Override
    public void deleteVariant(Long id) {
        ProductVariant variant = productVariantRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND, "Variant not found with id: " + id));

        variant.setIsActive(false);
        productVariantRepository.save(variant);
    }

    private void validateSkuForCreate(String sku) {
        if (productVariantRepository.existsBySku(sku)) {
            throw new AppException(ErrorCode.PRODUCT_VARIANT_ALREADY_EXIST, "Product variant SKU already exists");
        }
    }

    private void validateSkuForUpdate(String sku, Long id) {
        if (productVariantRepository.existsBySkuAndIdNot(sku, id)) {
            throw new AppException(ErrorCode.PRODUCT_VARIANT_ALREADY_EXIST, "Product variant SKU already exists");
        }
    }

    private void validateVariantCombinationForCreate(Long productId, String color, String size) {
        if (productVariantRepository.existsByProductAndAttributes(productId, color, size)) {
            throw new AppException(ErrorCode.PRODUCT_VARIANT_ALREADY_EXIST, "Variant with the same product, color, and size already exists");
        }
    }

    private void validateVariantCombinationForUpdate(Long productId, String color, String size, Long id) {
        if (productVariantRepository.existsByProductAndAttributesExcludingId(productId, color, size, id)) {
            throw new AppException(ErrorCode.PRODUCT_VARIANT_ALREADY_EXIST, "Variant with the same product, color, and size already exists");
        }
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, message);
        }
        return value.trim();
    }

    private ProductVariantResponse mapToResponse(ProductVariant variant) {
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .productId(variant.getProduct() != null ? variant.getProduct().getId() : null)
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
