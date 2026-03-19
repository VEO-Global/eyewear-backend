package com.veo.backend.service;

import com.veo.backend.dto.request.ProductBundleRequest;
import com.veo.backend.dto.response.ProductBundleResponse;

import java.util.List;

public interface ProductBundleService {
    List<ProductBundleResponse> getAllBundles();
    ProductBundleResponse getBundleById(Long id);
    ProductBundleResponse createBundle(ProductBundleRequest request);
    ProductBundleResponse updateBundle(Long id, ProductBundleRequest request);
    void deleteBundle(Long id);
}