package com.veo.backend.dto.request;

import com.veo.backend.enums.OrderStatus;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {
    private OrderStatus status;
}
