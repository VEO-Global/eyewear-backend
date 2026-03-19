package com.veo.backend.dto.response;

import com.veo.backend.enums.OrderStatus;
import com.veo.backend.enums.OrderType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long orderId;
    private String customerEmail;
    private OrderStatus status;
    private OrderType orderType;
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private String shippingAddress;
    private String province;
    private String district;
    private String ward;
    private String addressDetail;
    private String phoneNumber;
    private String receiverName;
    private String note;
    private String logisticsProvider;
    private String trackingNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemResponse> items;
}
