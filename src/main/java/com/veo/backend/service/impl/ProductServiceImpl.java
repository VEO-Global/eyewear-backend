package com.veo.backend.service.impl;

import com.veo.backend.dto.request.ProductCreateRequest;
import com.veo.backend.dto.response.ProductCreateResponse;
import com.veo.backend.entity.Category;
import com.veo.backend.entity.Product;
import com.veo.backend.repository.CategoryRepository;
import com.veo.backend.repository.ProductRepository;
import com.veo.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public ProductCreateResponse createProduct(ProductCreateRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() ->new RuntimeException("Category not found with id: " + request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .brand(request.getBrand())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .material(request.getMaterial())
                .gender(request.getGender())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .createdAt(LocalDateTime.now())
                .category(category)
                .build();

        Product savedProduct = productRepository.save(product);

        return mapToResponse(savedProduct);
    }

    private ProductCreateResponse mapToResponse(Product product) {
        return ProductCreateResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .material(product.getMaterial())
                .gender(product.getGender())
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
