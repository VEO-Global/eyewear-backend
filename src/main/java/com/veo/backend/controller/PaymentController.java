package com.veo.backend.controller;

import com.veo.backend.dto.request.PaymentConfirmRequest;
import com.veo.backend.dto.request.PayosPaymentActionRequest;
import com.veo.backend.dto.response.PagedResponse;
import com.veo.backend.dto.response.PaymentQrResponse;
import com.veo.backend.dto.response.PaymentSummaryResponse;
import com.veo.backend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    @PostMapping("/payos/confirm")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentSummaryResponse> confirmFakePayosPayment(
            Authentication authentication,
            @RequestBody @Valid PayosPaymentActionRequest request
    ) {
        return ResponseEntity.ok(paymentService.confirmFakePayosPayment(authentication.getName(), request));
    }

    @PostMapping("/payos/approve")
    @PreAuthorize("hasAnyRole('SALES','MANAGER','ADMIN')")
    public ResponseEntity<PaymentSummaryResponse> approveFakePayosPayment(
            Authentication authentication,
            @RequestBody @Valid PayosPaymentActionRequest request
    ) {
        return ResponseEntity.ok(paymentService.approveFakePayosPayment(authentication.getName(), request));
    }

    @PostMapping("/payos/reject")
    @PreAuthorize("hasAnyRole('SALES','MANAGER','ADMIN')")
    public ResponseEntity<PaymentSummaryResponse> rejectFakePayosPayment(
            Authentication authentication,
            @RequestBody @Valid PayosPaymentActionRequest request
    ) {
        return ResponseEntity.ok(paymentService.rejectFakePayosPayment(authentication.getName(), request));
    }

    @GetMapping("/payos/pending")
    @PreAuthorize("hasAnyRole('SALES','MANAGER','ADMIN')")
    public ResponseEntity<List<PaymentSummaryResponse>> getPendingFakePayosPayments() {
        return ResponseEntity.ok(paymentService.getPendingFakePayosPayments());
    }

    @GetMapping(path = "/payos/stream/customer", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public SseEmitter subscribeCustomerPaymentEvents(Authentication authentication) {
        return paymentService.subscribeCustomerPaymentEvents(authentication.getName());
    }

    @GetMapping(path = "/payos/stream/staff", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('SALES','MANAGER','ADMIN')")
    public SseEmitter subscribeStaffPaymentEvents(Authentication authentication) {
        return paymentService.subscribeStaffPaymentEvents(authentication.getName());
    }

    @PostMapping("/order/{orderId}/confirm")
    @PreAuthorize("hasAnyRole('SALES','MANAGER','ADMIN')")
    public ResponseEntity<PaymentSummaryResponse> confirmPayment(
            Authentication auth,
            @PathVariable Long orderId,
            @RequestBody @Valid PaymentConfirmRequest request) {
        return ResponseEntity.ok(paymentService.confirmPayment(auth.getName(), orderId, request));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('SALES','MANAGER','ADMIN')")
    public ResponseEntity<List<PaymentSummaryResponse>> getPaymentsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.getPaymentsByUserId(userId));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('SALES','MANAGER','ADMIN')")
    public ResponseEntity<PagedResponse<PaymentSummaryResponse>> getAllPayments(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(paymentService.getAllPayments(pageable));
    }

    // ====== PayOS Return/Cancel ======

    @GetMapping("/payos/return")
    public ResponseEntity<String> handlePayosReturn(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Long orderCode,
            @RequestParam(required = false, defaultValue = "false") boolean cancel) {

        if (orderCode == null || cancel || !"00".equals(code)) {
            return ResponseEntity.status(302)
                    .header("Location", "http://localhost:5173/")
                    .build();
        }

        paymentService.handlePayosReturn(orderCode);

        String html = buildSuccessHtml(orderCode);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    @GetMapping("/payos/cancel")
    public ResponseEntity<Void> handlePayosCancel() {
        return ResponseEntity.status(302)
                .header("Location", "http://localhost:5173/")
                .build();
    }

    private String buildSuccessHtml(Long orderId) {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Thanh toán thành công - VEO Eyewear</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: 'Segoe UI', Tahoma, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                    }
                    .card {
                        background: white;
                        border-radius: 20px;
                        padding: 48px;
                        text-align: center;
                        max-width: 480px;
                        width: 90%%;
                        box-shadow: 0 20px 60px rgba(0,0,0,0.15);
                    }
                    .checkmark {
                        width: 80px; height: 80px;
                        border-radius: 50%%;
                        background: #4CAF50;
                        display: flex; align-items: center; justify-content: center;
                        margin: 0 auto 24px;
                        animation: scaleIn 0.5s ease;
                    }
                    .checkmark svg { width: 40px; height: 40px; fill: none; stroke: white; stroke-width: 3; stroke-linecap: round; stroke-linejoin: round; }
                    h1 { color: #2d3748; font-size: 24px; margin-bottom: 8px; }
                    .order-info { color: #718096; font-size: 16px; margin-bottom: 32px; }
                    .buttons { display: flex; gap: 12px; justify-content: center; flex-wrap: wrap; }
                    .btn {
                        padding: 14px 28px; border-radius: 12px; font-size: 15px;
                        font-weight: 600; text-decoration: none; cursor: pointer;
                        transition: all 0.2s; border: none;
                    }
                    .btn-primary {
                        background: linear-gradient(135deg, #667eea, #764ba2);
                        color: white;
                    }
                    .btn-primary:hover { transform: translateY(-2px); box-shadow: 0 4px 15px rgba(102,126,234,0.4); }
                    .btn-outline {
                        background: white; color: #667eea;
                        border: 2px solid #667eea;
                    }
                    .btn-outline:hover { background: #f7f8fc; transform: translateY(-2px); }
                    @keyframes scaleIn { 0%% { transform: scale(0); } 50%% { transform: scale(1.1); } 100%% { transform: scale(1); } }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="checkmark">
                        <svg viewBox="0 0 24 24"><polyline points="20 6 9 17 4 12"></polyline></svg>
                    </div>
                    <h1>Thanh toán thành công!</h1>
                    <p class="order-info">Mã đơn hàng: <strong>ORD-%d</strong></p>
                    <div class="buttons">
                        <a href="http://localhost:5173/" class="btn btn-outline">Về trang chủ</a>
                        <a href="http://localhost:5173/orders" class="btn btn-primary">Theo dõi đơn hàng</a>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(orderId);
    }
}
