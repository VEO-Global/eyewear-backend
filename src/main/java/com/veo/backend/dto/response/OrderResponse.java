package com.veo.backend.dto.response;

import com.veo.backend.enums.OrderStatus;
import com.veo.backend.enums.OrderType;
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
public class OrderResponse {
    private Long orderId;
    private String orderCode;
    private String customerEmail;
    private OrderStatus status;
    private OrderStatus orderStatus;
    private String statusLabel;
    private String customerTab;
    private OrderType orderType;
    private PrescriptionOption prescriptionOption;
    private PrescriptionReviewStatus prescriptionReviewStatus;
    private BigDecimal totalAmount;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private PaymentStatus paymentStatus;
    private String shippingAddress;
    private String city;
    private String district;
    private String ward;
    private String addressDetail;
    private String phoneNumber;
    private String receiverName;
    private String note;
    private String cancelReason;
    private String logisticsProvider;
    private String trackingNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private PaymentSummaryResponse payment;
    private LensSummaryResponse lens;
    private PrescriptionResponse prescription;
    private PriceSummaryResponse priceSummary;
    private List<OrderStatusHistoryResponse> statusHistory;
    private List<ReturnRequestSummaryResponse> returnRequests;
    private List<OrderItemResponse> items;
}
