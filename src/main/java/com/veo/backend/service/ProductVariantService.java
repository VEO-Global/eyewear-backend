package com.veo.backend.service;

import com.veo.backend.dto.request.BulkVariantStockUpdateRequest;
import com.veo.backend.dto.request.ProductVariantCreateRequest;
import com.veo.backend.dto.request.ProductVariantUpdateRequest;
import com.veo.backend.dto.response.ProductVariantResponse;

import java.util.List;

public interface ProductVariantService {
    List<ProductVariantResponse> getVariantsByProduct(Long productId, Boolean active);

    List<ProductVariantResponse> getVariants(Long productId, Boolean active);

    ProductVariantResponse getVariantById(Long id);

    ProductVariantResponse createVariant(ProductVariantCreateRequest request);

    List<ProductVariantResponse> createVariantsBulk(List<ProductVariantCreateRequest> requests);

    ProductVariantResponse updateVariant(Long id, ProductVariantUpdateRequest request);

    List<ProductVariantResponse> updateBulkStock(BulkVariantStockUpdateRequest request);

    void deleteVariant(Long id);
}
