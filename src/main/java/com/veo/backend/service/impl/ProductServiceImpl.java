package com.veo.backend.service.impl;

import com.veo.backend.dto.request.ProductCreateRequest;
import com.veo.backend.dto.request.ProductUpdateRequest;
import com.veo.backend.dto.response.ProductResponse;
import com.veo.backend.dto.response.ProductVariantResponse;
import com.veo.backend.entity.Category;
import com.veo.backend.entity.Product;
import com.veo.backend.entity.ProductImage;
import com.veo.backend.enums.ProductCatalogType;
import com.veo.backend.enums.ProductStatus;
import com.veo.backend.repository.CategoryRepository;
import com.veo.backend.repository.ProductRepository;
import com.veo.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public List<ProductResponse> getAllProducts(String status) {
        if (status == null || status.isBlank()) {
            return productRepository.findByIsActiveTrue()
                    .stream()
                    .sorted(productDisplayOrder())
                    .map(this::mapToResponse)
                    .toList();
        }

        ProductStatus productStatus = parseStatus(status);
        return productRepository.findByIsActiveTrueAndStatus(productStatus)
                .stream()
                .sorted(productDisplayOrder())
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<ProductResponse> getPreorderProducts() {
        return productRepository.findByIsActiveTrueAndCatalogType(ProductCatalogType.NEW)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public ProductResponse createProduct(ProductCreateRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .brand(request.getBrand())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .material(request.getMaterial())
                .gender(request.getGender())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .status(request.getStatus() != null ? request.getStatus() : ProductStatus.AVAILABLE)
                .createdAt(LocalDateTime.now())
                .category(category)
                .model3dUrl(request.getModel3dUrl())
                .catalogType(request.getCatalogType() != null ? request.getCatalogType() : ProductCatalogType.OLD)
                .build();

        Product savedProduct = productRepository.save(product);

        return mapToResponse(savedProduct);
    }

    @Override
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        return mapToResponse(product);
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        }

        if (request.getName() != null) product.setName(request.getName());
        if (request.getBrand() != null) product.setBrand(request.getBrand());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getBasePrice() != null) product.setBasePrice(request.getBasePrice());
        if (request.getMaterial() != null) product.setMaterial(request.getMaterial());
        if (request.getGender() != null) product.setGender(request.getGender());
        if (request.getModel3dUrl() != null) product.setModel3dUrl(request.getModel3dUrl());
        if (request.getIsActive() != null) product.setIsActive(request.getIsActive());
        if (request.getStatus() != null) product.setStatus(request.getStatus());
        if (request.getCatalogType() != null) product.setCatalogType(request.getCatalogType());

        return mapToResponse(productRepository.save(product));
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setIsActive(false);
        productRepository.save(product);
    }


    private ProductResponse mapToResponse(Product product) {

        List<ProductVariantResponse> variantResponses = null;
        if (product.getVariants() != null) {
            variantResponses = product.getVariants().stream()
                    .map(this::mapToVariantResponse)
                    .toList();
        }

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .material(product.getMaterial())
                .gender(product.getGender())
                .model3dUrl(product.getModel3dUrl())
                .imageUrl(resolveImageUrl(product))
                .status(product.getStatus())
                .catalogType(product.getCatalogType())
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .categoryId(product.getCategory().getId())
                .variants(variantResponses)
                .build();
    }

    private Comparator<Product> productDisplayOrder() {
        return Comparator
                .comparing((Product product) -> product.getCatalogType() == ProductCatalogType.NEW)
                .thenComparing(Product::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private ProductStatus parseStatus(String status) {
        try {
            return ProductStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid product status: " + status);
        }
    }

    private String resolveImageUrl(Product product) {
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }

        return product.getImages().stream()
                .filter(Objects::nonNull)
                .sorted((left, right) -> Boolean.compare(
                        right.getIsThumbnail() != null && right.getIsThumbnail(),
                        left.getIsThumbnail() != null && left.getIsThumbnail()))
                .map(ProductImage::getImageUrl)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private ProductVariantResponse mapToVariantResponse(com.veo.backend.entity.ProductVariant variant) {
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .color(variant.getColor())
                .size(variant.getSize())
                .price(variant.getPrice())
                .stockQuantity(variant.getStockQuantity())
                .isActive(variant.getIsActive())
                .build();
    }
}

