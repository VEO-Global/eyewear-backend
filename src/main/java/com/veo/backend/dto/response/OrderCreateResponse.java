package com.veo.backend.dto.response;

import com.veo.backend.enums.OrderStatus;
import com.veo.backend.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;
    private String message;
}
