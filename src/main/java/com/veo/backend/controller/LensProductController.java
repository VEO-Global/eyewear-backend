package com.veo.backend.controller;

import com.veo.backend.dto.request.LensProductUpdateRequest;
import com.veo.backend.dto.response.LensProductResponse;
import com.veo.backend.service.LensProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lens_products")
@RequiredArgsConstructor
public class LensProductController {
    private final LensProductService lensProductService;

    @GetMapping
    public List<LensProductResponse> getAllLensProducts() {
        return lensProductService.getAllLensProducts();
    }

    @GetMapping("/{id}")
    public LensProductResponse getLensProductById(@PathVariable Long id) {
        return lensProductService.getLensProductById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public LensProductResponse updateLensProduct(@PathVariable Long id, @RequestBody LensProductUpdateRequest request){
        return lensProductService.updateLensProduct(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public void deleteLensProductById(@PathVariable Long id) {
        lensProductService.deleteLensProduct(id);
    }
}
