package com.veo.backend.dto.request;

import com.veo.backend.enums.ReturnRequestType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReturnRequestCreateRequest {
    @NotNull(message = "Order id is required")
    private Long orderId;

    @NotNull(message = "Return type is required")
    private ReturnRequestType type;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String description;

    private String evidenceImageUrl;

    private BigDecimal requestedAmount;

    private String refundBankName;

    private String refundBankAccountNumber;

    private String refundBankAccountName;

    @NotEmpty(message = "Return items must not be empty")
    @Valid
    private List<ReturnRequestItemRequest> items;
}
