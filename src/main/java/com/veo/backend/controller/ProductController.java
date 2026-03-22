package com.veo.backend.controller;

import com.veo.backend.dto.response.FavoriteStatusResponse;
import com.veo.backend.dto.request.ProductCreateRequest;
import com.veo.backend.dto.request.ProductUpdateRequest;
import com.veo.backend.dto.response.ProductResponse;
import com.veo.backend.service.FavoriteProductService;
import com.veo.backend.service.ProductService;
import com.veo.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/products", "/products"})
@RequiredArgsConstructor
public class ProductController {
    private final ProductService service;
    private final FavoriteProductService favoriteProductService;
    private final UserService userService;

    @GetMapping
    public List<ProductResponse> getAllProducts(@RequestParam(required = false) String status) {
        return service.getAllProducts(status);
    }

    @GetMapping("/preorder")
    public List<ProductResponse> getPreorderProducts() {
        return service.getPreorderProducts();
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@RequestBody ProductCreateRequest request) {
        return ResponseEntity.ok(service.createProduct(request));
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/{id}/favorite-status")
    public FavoriteStatusResponse getFavoriteStatus(@PathVariable("id") Long productId) {
        Long userId = userService.getMyProfile().getId();
        return favoriteProductService.getFavoriteStatus(userId, productId);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @RequestBody ProductUpdateRequest request) {
        return service.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deleteProduct(id);
    }
}
