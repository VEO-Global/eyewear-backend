package com.veo.backend.controller;

import com.veo.backend.dto.request.ProductBundleRequest;
import com.veo.backend.service.ProductBundleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bundles")
@RequiredArgsConstructor
public class ProductBundleController {
    private final ProductBundleService bundleService;

    @GetMapping
    public ResponseEntity<?> getAllBundles() {
        return ResponseEntity.ok(bundleService.getAllBundles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBundleById(@PathVariable Long id) {
        return ResponseEntity.ok(bundleService.getBundleById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> createBundle(@RequestBody ProductBundleRequest request) {
        return ResponseEntity.ok(bundleService.createBundle(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> updateBundle(@PathVariable Long id, @RequestBody ProductBundleRequest request) {
        return ResponseEntity.ok(bundleService.updateBundle(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> deleteBundle(@PathVariable Long id) {
        bundleService.deleteBundle(id);
        return ResponseEntity.noContent().build();
    }
}