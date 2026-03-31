package com.veo.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RevenuePointResponse {
    private String label;
    private long totalOrders;
    private BigDecimal totalRevenue;
}
