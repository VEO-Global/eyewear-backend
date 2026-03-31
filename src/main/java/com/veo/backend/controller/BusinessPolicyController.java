package com.veo.backend.controller;

import com.veo.backend.dto.request.BusinessPolicyUpdateRequest;
import com.veo.backend.dto.response.BusinessPolicyResponse;
import com.veo.backend.enums.BusinessPolicyType;
import com.veo.backend.service.BusinessPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BusinessPolicyController {
    private final BusinessPolicyService businessPolicyService;

    @GetMapping("/api/manager/policies")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<BusinessPolicyResponse>> getAllPolicies() {
        return ResponseEntity.ok(businessPolicyService.getAllPolicies());
    }

    @GetMapping("/api/manager/policies/{type}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<BusinessPolicyResponse> getPolicyForManager(@PathVariable BusinessPolicyType type) {
        return ResponseEntity.ok(businessPolicyService.getPolicyByType(type));
    }

    @PutMapping("/api/manager/policies/{type}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<BusinessPolicyResponse> updatePolicy(
            @PathVariable BusinessPolicyType type,
            Authentication authentication,
            @Valid @RequestBody BusinessPolicyUpdateRequest request) {
        return ResponseEntity.ok(businessPolicyService.updatePolicy(type, request, authentication.getName()));
    }

    @GetMapping("/api/public/policies/{type}")
    public ResponseEntity<BusinessPolicyResponse> getPublicPolicy(@PathVariable BusinessPolicyType type) {
        return ResponseEntity.ok(businessPolicyService.getPolicyByType(type));
    }
}
