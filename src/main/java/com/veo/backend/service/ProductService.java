package com.veo.backend.service;

import com.veo.backend.dto.request.ProductCreateRequest;
import com.veo.backend.dto.response.ProductCreateResponse;
import com.veo.backend.entity.Product;

import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();

    ProductCreateResponse createProduct(ProductCreateRequest request);
}
