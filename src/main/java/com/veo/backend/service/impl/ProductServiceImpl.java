package com.veo.backend.service.impl;

import com.veo.backend.dto.request.ProductCreateRequest;
import com.veo.backend.dto.request.ProductImageRequest;
import com.veo.backend.dto.request.ProductUpdateRequest;
import com.veo.backend.dto.response.ProductResponse;
import com.veo.backend.entity.Category;
import com.veo.backend.entity.Product;
import com.veo.backend.entity.ProductImage;
import com.veo.backend.enums.ProductCatalogType;
import com.veo.backend.enums.ProductStatus;
import com.veo.backend.repository.CategoryRepository;
import com.veo.backend.repository.ProductImageRepository;
import com.veo.backend.repository.ProductRepository;
import com.veo.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductResponseMapper productResponseMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts(String status) {
        if (status == null || status.isBlank()) {
            return productRepository.findByIsActiveTrue()
                    .stream()
                    .sorted(productDisplayOrder())
                    .map(productResponseMapper::map)
                    .toList();
        }

        ProductStatus productStatus = parseStatus(status);
        return productRepository.findByIsActiveTrueAndStatus(productStatus)
                .stream()
                .sorted(productDisplayOrder())
                .map(productResponseMapper::map)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getPreorderProducts() {
        return productRepository.findByIsActiveTrueAndCatalogType(ProductCatalogType.NEW)
                .stream()
                .map(productResponseMapper::map)
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

        product.setImages(buildProductImages(product, request.getImages()));

        Product savedProduct = productRepository.save(product);

        return productResponseMapper.map(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        return productResponseMapper.map(product);
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
        if (request.getImages() != null) product.setImages(buildProductImages(product, request.getImages()));

        return productResponseMapper.map(productRepository.save(product));
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setIsActive(false);
        productRepository.save(product);
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

    private List<ProductImage> buildProductImages(Product product, List<ProductImageRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return new ArrayList<>();
        }

        List<ProductImageRequest> normalizedRequests = requests.stream()
                .filter(Objects::nonNull)
                .filter(request -> request.getUrl() != null && !request.getUrl().isBlank())
                .toList();

        if (normalizedRequests.isEmpty()) {
            return new ArrayList<>();
        }

        long primaryCount = normalizedRequests.stream()
                .filter(request -> Boolean.TRUE.equals(request.getIsPrimary()))
                .count();
        if (primaryCount == 0) {
            normalizedRequests.get(0).setIsPrimary(true);
        }

        List<ProductImage> productImages = new ArrayList<>();
        for (int index = 0; index < normalizedRequests.size(); index++) {
            ProductImageRequest request = normalizedRequests.get(index);
            ProductImage productImage = request.getId() == null
                    ? new ProductImage()
                    : resolveExistingProductImage(product, request.getId());

            productImage.setProduct(product);
            productImage.setImageUrl(request.getUrl().trim());
            productImage.setAltText(trimToNull(request.getAlt()));
            boolean isPrimary = primaryCount == 0
                    ? index == 0
                    : Boolean.TRUE.equals(request.getIsPrimary()) && productImages.stream().noneMatch(image -> Boolean.TRUE.equals(image.getIsPrimary()));
            productImage.setIsPrimary(isPrimary);
            productImage.setIsThumbnail(isPrimary);
            productImage.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : index);
            productImages.add(productImage);
        }

        if (productImages.stream().noneMatch(image -> Boolean.TRUE.equals(image.getIsPrimary()))) {
            productImages.get(0).setIsPrimary(true);
        }

        for (ProductImage image : productImages) {
            if (!Boolean.TRUE.equals(image.getIsPrimary())) {
                image.setIsPrimary(false);
                image.setIsThumbnail(false);
            } else {
                image.setIsThumbnail(true);
            }
        }

        return productImages;
    }

    private ProductImage resolveExistingProductImage(Product product, Long imageId) {
        ProductImage productImage = productImageRepository.findById(imageId).orElse(new ProductImage());
        if (productImage.getId() != null
                && productImage.getProduct() != null
                && product.getId() != null
                && !product.getId().equals(productImage.getProduct().getId())) {
            throw new RuntimeException("Product image does not belong to this product");
        }
        return productImage;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

