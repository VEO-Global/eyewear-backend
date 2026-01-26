package com.veo.backend.controller;

import com.veo.backend.dto.request.ProductVariantCreateRequest;
import com.veo.backend.dto.request.ProductVariantUpdateRequest;
import com.veo.backend.dto.response.ProductVariantResponse;
import com.veo.backend.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/variants")
@RequiredArgsConstructor
public class ProductVariantController {
    private final ProductVariantService variantService;

    @GetMapping("/product/{productId}")
    public List<ProductVariantResponse> getByProduct(@PathVariable Long productId) {
        return variantService.getVariantsByProduct(productId);
    }

    @GetMapping("/{id}")
    public ProductVariantResponse getById(@PathVariable Long id) {
        return variantService.getVariantById(id);
    }

    @PostMapping
    public ProductVariantResponse create(@RequestBody ProductVariantCreateRequest request) {
        return variantService.createVariant(request);
    }

    @PutMapping("/{id}")
    public ProductVariantResponse update(
            @PathVariable Long id,
            @RequestBody ProductVariantUpdateRequest request) {
        return variantService.updateVariant(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        variantService.deleteVariant(id);
    }
}
