package com.veo.backend.controller;

import com.veo.backend.dto.response.RevenuePointResponse;
import com.veo.backend.dto.response.RevenueSummaryResponse;
import com.veo.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/manager/revenue")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class RevenueController {
    private final PaymentService paymentService;

    @GetMapping("/summary")
    public ResponseEntity<RevenueSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(paymentService.getRevenueSummary(from, to));
    }

    @GetMapping("/daily")
    public ResponseEntity<List<RevenuePointResponse>> getDaily(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(paymentService.getRevenueDaily(year, month));
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<RevenuePointResponse>> getMonthly(@RequestParam int year) {
        return ResponseEntity.ok(paymentService.getRevenueMonthly(year));
    }
}
