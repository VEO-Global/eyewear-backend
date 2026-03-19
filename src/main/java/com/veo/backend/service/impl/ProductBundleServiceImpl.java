package com.veo.backend.service.impl;

import com.veo.backend.dto.request.ProductBundleRequest;
import com.veo.backend.dto.response.ProductBundleResponse;
import com.veo.backend.entity.ProductBundle;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.ProductBundleRepository;
import com.veo.backend.service.ProductBundleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductBundleServiceImpl implements ProductBundleService {
    private final ProductBundleRepository repository;

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
        return ProductBundleResponse.builder()
                .id(bundle.getId())
                .name(bundle.getName())
                .description(bundle.getDescription())
                .bundlePrice(bundle.getBundlePrice())
                .isActive(bundle.isActive())
                .productVariantIds(bundle.getProductVariantIds())
                .lensProductIds(bundle.getLensProductIds())
                .build();
    }
}
