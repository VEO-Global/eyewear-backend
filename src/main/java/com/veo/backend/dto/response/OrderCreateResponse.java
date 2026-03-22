package com.veo.backend.dto.response;

import com.veo.backend.enums.OrderStatus;
import com.veo.backend.enums.PaymentStatus;
import com.veo.backend.enums.PrescriptionOption;
import com.veo.backend.enums.PrescriptionReviewStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderCreateResponse {
    private Long orderId;
    private String orderCode;
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private PrescriptionOption prescriptionOption;
    private PrescriptionReviewStatus prescriptionReviewStatus;
    private List<OrderItemResponse> items;
    private LensSummaryResponse lens;
    private PrescriptionResponse prescription;
    private PriceSummaryResponse priceSummary;
    private LocalDateTime createdAt;
    private String message;
}
