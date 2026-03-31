package com.veo.backend.service.impl;

import com.veo.backend.dto.request.ProductBundleRequest;
import com.veo.backend.dto.response.LensProductResponse;
import com.veo.backend.dto.response.ProductBundleResponse;
import com.veo.backend.dto.response.ProductVariantResponse;
import com.veo.backend.entity.LensProduct;
import com.veo.backend.entity.ProductBundle;
import com.veo.backend.entity.ProductVariant;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.LensProductRepository;
import com.veo.backend.repository.ProductBundleRepository;
import com.veo.backend.repository.ProductVariantRepository;
import com.veo.backend.service.ProductBundleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductBundleServiceImpl implements ProductBundleService {
    private final ProductBundleRepository repository;
    private final ProductVariantRepository productVariantRepository;
    private final LensProductRepository lensProductRepository;

    @Override
    public List<ProductBundleResponse> getAllBundles() {
        return repository.findByIsActiveTrue().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public ProductBundleResponse getBundleById(Long id) {
        return repository.findById(id).map(this::mapToResponse)
                .orElseThrow(() -> new AppException(ErrorCode.VALIDATION_ERROR, "Bundle not found"));
    }

    @Override
    public ProductBundleResponse createBundle(ProductBundleRequest request) {
        validateBundleReferences(request.getProductVariantIds(), request.getLensProductIds());
        ProductBundle bundle = new ProductBundle();
        bundle.setName(request.getName());
        bundle.setDescription(request.getDescription());
        bundle.setBundlePrice(request.getBundlePrice());
        bundle.setActive(request.getIsActive() != null ? request.getIsActive() : true);
        bundle.setProductVariantIds(request.getProductVariantIds());
        bundle.setLensProductIds(request.getLensProductIds());
        ProductBundle saved = repository.save(bundle);
        return mapToResponse(saved);
    }

    @Override
    public ProductBundleResponse updateBundle(Long id, ProductBundleRequest request) {
        ProductBundle bundle = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VALIDATION_ERROR, "Bundle not found"));
        validateBundleReferences(request.getProductVariantIds(), request.getLensProductIds());
        if (request.getName() != null) bundle.setName(request.getName());
        if (request.getDescription() != null) bundle.setDescription(request.getDescription());
        if (request.getBundlePrice() != null) bundle.setBundlePrice(request.getBundlePrice());
        if (request.getIsActive() != null) bundle.setActive(request.getIsActive());
        if (request.getProductVariantIds() != null) bundle.setProductVariantIds(request.getProductVariantIds());
        if (request.getLensProductIds() != null) bundle.setLensProductIds(request.getLensProductIds());
        return mapToResponse(repository.save(bundle));
    }

    @Override
    public void deleteBundle(Long id) {
        ProductBundle bundle = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VALIDATION_ERROR, "Bundle not found"));
        bundle.setActive(false);
        repository.save(bundle);
    }

    private ProductBundleResponse mapToResponse(ProductBundle bundle) {
        List<ProductVariantResponse> variantDetails = bundle.getProductVariantIds() == null
                ? List.of()
                : bundle.getProductVariantIds().stream()
                .map(this::findActiveVariant)
                .filter(Objects::nonNull)
                .map(this::mapVariant)
                .toList();

        List<LensProductResponse> lensDetails = bundle.getLensProductIds() == null
                ? List.of()
                : bundle.getLensProductIds().stream()
                .map(this::findActiveLens)
                .filter(Objects::nonNull)
                .map(this::mapLens)
                .toList();

        return ProductBundleResponse.builder()
                .id(bundle.getId())
                .name(bundle.getName())
                .description(bundle.getDescription())
                .bundlePrice(bundle.getBundlePrice())
                .isActive(bundle.isActive())
                .productVariantIds(bundle.getProductVariantIds())
                .lensProductIds(bundle.getLensProductIds())
                .productVariants(variantDetails)
                .lensProducts(lensDetails)
                .build();
    }

    private void validateBundleReferences(List<Long> variantIds, List<Long> lensIds) {
        if (variantIds != null) {
            variantIds.forEach(variantId -> {
                ProductVariant variant = findActiveVariant(variantId);
                if (variant == null) {
                    throw new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND, "Product variant not found or inactive: " + variantId);
                }
            });
        }

        if (lensIds != null) {
            lensIds.forEach(lensId -> {
                LensProduct lensProduct = findActiveLens(lensId);
                if (lensProduct == null) {
                    throw new AppException(ErrorCode.LENS_PRODUCT_NOT_FOUND, "Lens product not found or inactive: " + lensId);
                }
            });
        }
    }

    private ProductVariant findActiveVariant(Long variantId) {
        return productVariantRepository.findById(variantId)
                .filter(variant -> Boolean.TRUE.equals(variant.getIsActive()))
                .orElse(null);
    }

    private LensProduct findActiveLens(Long lensId) {
        return lensProductRepository.findById(lensId)
                .filter(lensProduct -> Boolean.TRUE.equals(lensProduct.getIsActive()))
                .orElse(null);
    }

    private ProductVariantResponse mapVariant(ProductVariant variant) {
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .productId(variant.getProduct() != null ? variant.getProduct().getId() : null)
                .sku(variant.getSku())
                .color(variant.getColor())
                .size(variant.getSize())
                .price(variant.getPrice())
                .stockQuantity(variant.getStockQuantity())
                .expectedRestockDate(variant.getExpectedRestockDate())
                .isActive(Boolean.TRUE.equals(variant.getIsActive()))
                .build();
    }

    private LensProductResponse mapLens(LensProduct lensProduct) {
        return LensProductResponse.builder()
                .id(lensProduct.getId())
                .name(lensProduct.getName())
                .type(lensProduct.getType())
                .refractionIndex(lensProduct.getRefractionIndex())
                .description(lensProduct.getDescription())
                .price(lensProduct.getPrice())
                .isActive(lensProduct.getIsActive())
                .build();
    }
}
