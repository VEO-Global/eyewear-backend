package com.veo.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PriceSummaryResponse {
    private BigDecimal itemsSubtotal;
    private BigDecimal lensPrice;
    private BigDecimal shippingFee;
    private BigDecimal total;
}
