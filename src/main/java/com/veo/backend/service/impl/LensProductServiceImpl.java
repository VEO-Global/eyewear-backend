package com.veo.backend.service.impl;

import com.veo.backend.dto.request.LensProductCreateRequest;
import com.veo.backend.dto.request.LensProductUpdateRequest;
import com.veo.backend.dto.response.LensProductResponse;
import com.veo.backend.entity.LensProduct;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.LensProductRepository;
import com.veo.backend.service.LensProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LensProductServiceImpl implements LensProductService {
    private final LensProductRepository lensProductRepository;

    @Override
    public LensProductResponse createLensProduct(LensProductCreateRequest request) {
        LensProduct lensProduct = new LensProduct();
        lensProduct.setName(request.getName().trim());
        lensProduct.setType(request.getType().trim());
        lensProduct.setRefractionIndex(request.getRefractionIndex());
        lensProduct.setDescription(request.getDescription());
        lensProduct.setPrice(request.getPrice());
        lensProduct.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        return mapToResponse(lensProductRepository.save(lensProduct));
    }

    @Override
    public LensProductResponse updateLensProduct(Long id, LensProductUpdateRequest request) {
        LensProduct lensProduct = lensProductRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LENS_PRODUCT_NOT_FOUND, "Lens product not found"));

        if (lensProduct.getIsActive() == false)
            throw new AppException(ErrorCode.LENS_PRODUCT_NOT_VALID, "Lens product not active");

        lensProduct.setDescription(request.getDescription());
        lensProduct.setPrice(request.getPrice());

        return mapToResponse(lensProductRepository.save(lensProduct));
    }

    @Override
    public void deleteLensProduct(Long id) {
        LensProduct lensProduct = lensProductRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LENS_PRODUCT_NOT_FOUND, "Lens product not found"));

        lensProduct.setIsActive(false);
        lensProductRepository.save(lensProduct);
    }

    @Override
    public LensProductResponse getLensProductById(Long id) {
        LensProduct lensProduct = lensProductRepository.findById(id)
                .orElseThrow(() ->  new AppException(ErrorCode.LENS_PRODUCT_NOT_FOUND, "Lens product not found"));

        if (lensProduct.getIsActive() == false)
            throw new AppException(ErrorCode.LENS_PRODUCT_NOT_VALID, "Lens product not active");

        return mapToResponse(lensProduct);
    }

    @Override
    public List<LensProductResponse> getAllLensProducts() {
        return lensProductRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private LensProductResponse mapToResponse(LensProduct lensProduct) {
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
