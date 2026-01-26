package com.veo.backend.service;

import com.veo.backend.dto.request.ProductVariantCreateRequest;
import com.veo.backend.dto.request.ProductVariantUpdateRequest;
import com.veo.backend.dto.response.ProductVariantResponse;

import java.util.List;

public interface ProductVariantService {
    List<ProductVariantResponse> getVariantsByProduct(Long productId);

    ProductVariantResponse getVariantById(Long id);

    ProductVariantResponse createVariant(ProductVariantCreateRequest request);

    ProductVariantResponse updateVariant(Long id, ProductVariantUpdateRequest request);

    void deleteVariant(Long id);
}
