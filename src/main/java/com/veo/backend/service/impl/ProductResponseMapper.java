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
        List<ProductImageResponse> imageResponses = product.getImages() == null
                ? List.of()
                : product.getImages().stream()
                .filter(Objects::nonNull)
                .sorted(productImageOrder())
                .map(this::mapImage)
                .toList();

        List<ProductVariantResponse> variantResponses = product.getVariants() == null
                ? List.of()
                : product.getVariants().stream()
                .filter(Objects::nonNull)
                .map(this::mapVariant)
                .toList();

        String resolvedImageUrl = resolveImageUrl(product);

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
                .status(product.getStatus())
                .catalogType(product.getCatalogType())
                .stockQuantity(resolveStockQuantity(product))
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .images(imageResponses)
                .variants(variantResponses)
                .build();
    }

    private ProductImageResponse mapImage(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .url(image.getImageUrl())
                .alt(image.getAltText())
                .isPrimary(Boolean.TRUE.equals(image.getIsPrimary()))
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
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }

        return product.getImages().stream()
                .filter(Objects::nonNull)
                .sorted(productImageOrder())
                .map(ProductImage::getImageUrl)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Comparator<ProductImage> productImageOrder() {
        return Comparator
                .comparing((ProductImage image) -> !Boolean.TRUE.equals(image.getIsPrimary()))
                .thenComparing(image -> image.getSortOrder() == null ? 0 : image.getSortOrder())
                .thenComparing(ProductImage::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private Integer resolveStockQuantity(Product product) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return 0;
        }

        return product.getVariants().stream()
                .filter(Objects::nonNull)
                .filter(variant -> Boolean.TRUE.equals(variant.getIsActive()))
                .map(ProductVariant::getStockQuantity)
                .filter(Objects::nonNull)
                .reduce(0, Integer::sum);
    }
}
