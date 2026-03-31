package com.veo.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OperationOrderSummaryResponse {
    private long totalOrders;
    private long waitingForStock;
    private long manufacturing;
    private long packing;
    private long readyToShip;
    private long shipping;
    private long completed;
    private long normalOrders;
    private long prescriptionOrders;
    private long preorderOrders;
}
