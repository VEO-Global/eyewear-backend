package com.veo.backend.service.impl;

import com.veo.backend.dto.response.ProductImageResponse;
import com.veo.backend.dto.response.ProductResponse;
import com.veo.backend.dto.response.ProductVariantResponse;
import com.veo.backend.entity.Product;
import com.veo.backend.entity.ProductImage;
import com.veo.backend.entity.ProductVariant;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Comparator;

@Component
public class ProductResponseMapper {
    public ProductResponse map(Product product) {
        List<ProductImageResponse> imageResponses = safeImages(product).stream()
                .filter(Objects::nonNull)
                .sorted(productImageOrder())
                .map(this::mapImage)
                .toList();

        List<ProductVariantResponse> variantResponses = safeVariants(product).stream()
                .filter(Objects::nonNull)
                .map(this::mapVariant)
                .toList();

        String resolvedImageUrl = resolveImageUrl(product);
        List<String> resolvedImageUrls = imageResponses.stream()
                .map(ProductImageResponse::getUrl)
                .filter(Objects::nonNull)
                .toList();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .material(product.getMaterial())
                .gender(product.getGender())
                .model3dUrl(product.getModel3dUrl())
                .imageUrl(resolvedImageUrl)
                .image(resolvedImageUrl)
                .imageUrls(resolvedImageUrls)
                .status(product.getStatus())
                .catalogType(product.getCatalogType())
                .stockQuantity(resolveStockQuantity(product))
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .categoryId(resolveCategoryId(product))
                .images(imageResponses)
                .variants(variantResponses)
                .build();
    }

    private ProductImageResponse mapImage(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .url(image.getImageUrl())
                .alt(image.getAltText())
                .isPrimary(resolvePrimaryFlag(image))
                .sortOrder(image.getSortOrder() == null ? 0 : image.getSortOrder())
                .build();
    }

    private ProductVariantResponse mapVariant(ProductVariant variant) {
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .color(variant.getColor())
                .size(variant.getSize())
                .price(variant.getPrice())
                .stockQuantity(variant.getStockQuantity())
                .expectedRestockDate(variant.getExpectedRestockDate())
                .isActive(Boolean.TRUE.equals(variant.getIsActive()))
                .build();
    }

    private String resolveImageUrl(Product product) {
        List<ProductImage> images = safeImages(product);
        if (images.isEmpty()) {
            return null;
        }

        return images.stream()
                .filter(Objects::nonNull)
                .sorted(productImageOrder())
                .map(ProductImage::getImageUrl)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Comparator<ProductImage> productImageOrder() {
        return Comparator
                .comparing((ProductImage image) -> !resolvePrimaryFlag(image))
                .thenComparing(image -> image.getSortOrder() == null ? 0 : image.getSortOrder())
                .thenComparing(ProductImage::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private boolean resolvePrimaryFlag(ProductImage image) {
        return Boolean.TRUE.equals(image.getIsPrimary()) || Boolean.TRUE.equals(image.getIsThumbnail());
    }

    private Integer resolveStockQuantity(Product product) {
        List<ProductVariant> variants = safeVariants(product);
        if (variants.isEmpty()) {
            return 0;
        }

        return variants.stream()
                .filter(Objects::nonNull)
                .filter(variant -> Boolean.TRUE.equals(variant.getIsActive()))
                .map(ProductVariant::getStockQuantity)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);
    }

    private List<ProductImage> safeImages(Product product) {
        try {
            return product.getImages() == null ? List.of() : product.getImages();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<ProductVariant> safeVariants(Product product) {
        try {
            return product.getVariants() == null ? List.of() : product.getVariants();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private Long resolveCategoryId(Product product) {
        try {
            return product.getCategory() != null ? product.getCategory().getId() : null;
        } catch (Exception ex) {
            return null;
        }
    }
}
