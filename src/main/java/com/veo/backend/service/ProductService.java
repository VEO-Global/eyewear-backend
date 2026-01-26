package com.veo.backend.service;

import com.veo.backend.dto.request.ProductCreateRequest;
import com.veo.backend.dto.request.ProductUpdateRequest;
import com.veo.backend.dto.response.ProductResponse;
import com.veo.backend.entity.Product;

import java.util.List;

public interface ProductService {
    List<ProductResponse> getAllProducts();

    ProductResponse createProduct(ProductCreateRequest request);

    ProductResponse getById(Long id);

    ProductResponse updateProduct(Long id, ProductUpdateRequest request);

    void deleteProduct(Long id);
}
