package com.veo.backend.controller;

import com.veo.backend.dto.request.StaffOrderPhaseUpdateRequest;
import com.veo.backend.dto.response.StaffOrderResponse;
import com.veo.backend.service.StaffOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/orders")
@RequiredArgsConstructor
public class StaffOrderController {
    private final StaffOrderService staffOrderService;

    @GetMapping
    public List<StaffOrderResponse> getOrders(
            @RequestParam(required = false) String phase,
            @RequestParam(required = false) String status
    ) {
        return staffOrderService.getOrders(phase, status);
    }

    @GetMapping("/{id}")
    public StaffOrderResponse getOrderDetail(@PathVariable Long id) {
        return staffOrderService.getOrderDetail(id);
    }

    @PatchMapping("/{id}/phase")
    public StaffOrderResponse updateOrderPhase(
            @PathVariable Long id,
            @Valid @RequestBody StaffOrderPhaseUpdateRequest request,
            Authentication authentication
    ) {
        return staffOrderService.updateOrderPhase(id, authentication.getName(), request);
    }

    @PatchMapping("/{id}/confirm")
    public StaffOrderResponse confirmOrder(@PathVariable Long id, Authentication authentication) {
        return staffOrderService.confirmOrder(id, authentication.getName());
    }

    @PatchMapping("/{id}/handoff")
    public StaffOrderResponse handoffOrder(@PathVariable Long id, Authentication authentication) {
        return staffOrderService.handoffOrder(id, authentication.getName());
    }

    @PatchMapping("/{id}/complete")
    public StaffOrderResponse completeOrder(@PathVariable Long id, Authentication authentication) {
        return staffOrderService.completeOrder(id, authentication.getName());
    }
}
