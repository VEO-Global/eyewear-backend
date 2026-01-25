package com.veo.backend.controller;

import com.veo.backend.dto.request.ProductCreateRequest;
import com.veo.backend.dto.response.ProductCreateResponse;
import com.veo.backend.entity.Product;
import com.veo.backend.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService productService) {
        this.service = productService;
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return service.getAllProducts();
    }

    @PostMapping
    public ResponseEntity<ProductCreateResponse> createProduct(@RequestBody ProductCreateRequest request) {
        return ResponseEntity.ok(service.createProduct(request));
    }
}
