package com.veo.backend.service;

import com.veo.backend.dto.request.LensProductUpdateRequest;
import com.veo.backend.dto.response.LensProductResponse;

import java.util.List;

public interface LensProductService {
    LensProductResponse updateLensProduct(Long id, LensProductUpdateRequest request);

    void deleteLensProduct(Long id);

    LensProductResponse getLensProductById(Long id);

    List<LensProductResponse> getAllLensProducts();
}
