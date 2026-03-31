package com.veo.backend.service;

import com.veo.backend.dto.request.StaffOrderPhaseUpdateRequest;
import com.veo.backend.dto.response.StaffOrderResponse;

import java.util.List;

public interface StaffOrderService {
    List<StaffOrderResponse> getOrders(String phase, String status);

    StaffOrderResponse getOrderDetail(Long id);

    StaffOrderResponse updateOrderPhase(Long id, String actorEmail, StaffOrderPhaseUpdateRequest request);

    StaffOrderResponse confirmOrder(Long id, String actorEmail);

    StaffOrderResponse handoffOrder(Long id, String actorEmail);

    StaffOrderResponse completeOrder(Long id, String actorEmail);
}
