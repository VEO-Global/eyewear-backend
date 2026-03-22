package com.veo.backend.dto.response;

import com.veo.backend.enums.ReturnRequestStatus;
import com.veo.backend.enums.ReturnRequestType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReturnRequestSummaryResponse {
    private Long returnRequestId;
    private Long orderId;
    private String orderCode;
    private ReturnRequestType type;
    private ReturnRequestStatus status;
    private String reason;
    private String description;
    private String evidenceImageUrl;
    private BigDecimal requestedAmount;
    private BigDecimal approvedAmount;
    private String refundBankName;
    private String refundBankAccountNumber;
    private String refundBankAccountName;
    private String staffNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ReturnRequestSummaryItemResponse> items;
}
