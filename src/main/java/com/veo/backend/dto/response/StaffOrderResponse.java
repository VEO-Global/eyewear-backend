package com.veo.backend.dto.response;

import com.veo.backend.enums.PaymentStatus;
import com.veo.backend.enums.PrescriptionOption;
import com.veo.backend.enums.PrescriptionReviewStatus;
import com.veo.backend.enums.StaffOrderPhase;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class StaffOrderResponse {
    private Long id;
    private String orderNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private StaffOrderPhase phase;
    private String phaseLabel;
    private String status;
    private PaymentStatus paymentStatus;
    private BigDecimal totalAmount;
    private String receiverName;
    private String phoneNumber;
    private String note;
    private Boolean requiresPrescription;
    private PrescriptionOption prescriptionOption;
    private PrescriptionReviewStatus prescriptionReviewStatus;
    private StaffOrderCustomerProfileResponse customerProfile;
    private String shippingAddress;
    private String sourceChannel;
    private LensSummaryResponse lensProduct;
    private PrescriptionResponse prescription;
    private PaymentSummaryResponse payment;
    private List<OrderItemResponse> items;
}
