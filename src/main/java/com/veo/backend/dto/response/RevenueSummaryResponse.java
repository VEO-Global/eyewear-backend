package com.veo.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RevenueSummaryResponse {
    private String fromDate;
    private String toDate;
    private long totalPaidOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
}
