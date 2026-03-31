package com.veo.backend.controller;

import com.veo.backend.dto.request.PaymentConfirmRequest;
import com.veo.backend.dto.request.PaymentRequest;
import com.veo.backend.dto.response.PaymentQrResponse;
import com.veo.backend.dto.response.PaymentSummaryResponse;
import com.veo.backend.service.PaymentService;
import io.jsonwebtoken.Jwt;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping("/order/{orderId}/qr")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentQrResponse> getQr(Authentication auth, @PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentQr(auth.getName(), orderId));
    }

    @GetMapping("/order/{orderId}/status")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentSummaryResponse> getPaymentStatus(Authentication auth, @PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(auth.getName(), orderId));
    }

    @GetMapping("/my-history")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<PaymentSummaryResponse>> getMyPayments(Authentication authentication) {
        return ResponseEntity.ok(paymentService.getMyPayments(authentication.getName()));
    }

    @PostMapping("/customer-pay")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<String> payOrder(
            @RequestBody PaymentRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        String message = paymentService.processPayment(request, email);
        return ResponseEntity.ok(message);
    }

    @PostMapping("/order/{orderId}/confirm")
    @PreAuthorize("hasRole('SALE')")
    public ResponseEntity<PaymentSummaryResponse> confirmPayment(
            Authentication auth,
            @PathVariable Long orderId,
            @RequestBody @Valid PaymentConfirmRequest request) {
        return ResponseEntity.ok(paymentService.confirmPayment(auth.getName(), orderId, request));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('SALE')")
    public ResponseEntity<List<PaymentSummaryResponse>> getPaymentsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.getPaymentsByUserId(userId));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('SALE')")
    public ResponseEntity<List<PaymentSummaryResponse>> getAllPayments(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(paymentService.getAllPayments(pageable));
    }
}
