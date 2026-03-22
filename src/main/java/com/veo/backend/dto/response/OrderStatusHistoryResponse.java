package com.veo.backend.dto.response;

import com.veo.backend.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrderStatusHistoryResponse {
    private Long id;
    private OrderStatus status;
    private String statusLabel;
    private String note;
    private LocalDateTime createdAt;
}
