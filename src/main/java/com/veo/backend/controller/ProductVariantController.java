package com.veo.backend.controller;

import com.veo.backend.dto.request.BulkVariantStockUpdateRequest;
import com.veo.backend.dto.request.ProductVariantCreateRequest;
import com.veo.backend.dto.request.ProductVariantUpdateRequest;
import com.veo.backend.dto.response.ProductVariantResponse;
import com.veo.backend.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/variants")
@RequiredArgsConstructor
public class ProductVariantController {
    private final ProductVariantService variantService;

    @GetMapping
    public List<ProductVariantResponse> getVariants(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Boolean active) {
        return variantService.getVariants(productId, active);
    }

    @GetMapping("/product/{productId}")
    public List<ProductVariantResponse> getByProduct(
            @PathVariable Long productId,
            @RequestParam(required = false, defaultValue = "true") Boolean active) {
        return variantService.getVariantsByProduct(productId, active);
    }

    @GetMapping("/{id}")
    public ProductVariantResponse getById(@PathVariable Long id) {
        return variantService.getVariantById(id);
    }

    @PostMapping
    public ProductVariantResponse create(@Valid @RequestBody ProductVariantCreateRequest request) {
        return variantService.createVariant(request);
    }

    @PostMapping("/bulk")
    public List<ProductVariantResponse> createBulk(@Valid @RequestBody List<@Valid ProductVariantCreateRequest> requests) {
        return variantService.createVariantsBulk(requests);
    }

    @PutMapping("/{id}")
    public ProductVariantResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProductVariantUpdateRequest request) {
        return variantService.updateVariant(id, request);
    }

    @PatchMapping("/bulk-stock")
    public List<ProductVariantResponse> updateBulkStock(@Valid @RequestBody BulkVariantStockUpdateRequest request) {
        return variantService.updateBulkStock(request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        variantService.deleteVariant(id);
    }
}
