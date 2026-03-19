package com.veo.backend.controller;

import com.veo.backend.dto.request.VirtualTryOnRequest;
import com.veo.backend.service.VirtualTryOnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/try-on")
@RequiredArgsConstructor
public class VirtualTryOnController {
    private final VirtualTryOnService virtualTryOnService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','MANAGER','SALES','OPERATIONS')")
    public ResponseEntity<?> createTryOn(@RequestBody VirtualTryOnRequest request, org.springframework.security.core.Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(virtualTryOnService.createTryOnSession(email, request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','MANAGER','SALES','OPERATIONS')")
    public ResponseEntity<?> getMySessions(org.springframework.security.core.Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(virtualTryOnService.getMySessions(email));
    }
}
