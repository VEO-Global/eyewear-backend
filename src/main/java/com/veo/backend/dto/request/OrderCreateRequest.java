package com.veo.backend.dto.request;

import com.veo.backend.enums.OrderType;
import com.veo.backend.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateRequest {
    @NotNull(message = "Order type is required")
    private OrderType orderType;

    private PaymentMethod paymentMethod;

    private String province;

    private String district;

    private String ward;

    @NotBlank(message = "Address detail is required")
    private String addressDetail;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Receiver name is required")
    private String receiverName;

    private String note;

    @NotEmpty(message = "Order items must not be empty")
    @Valid
    private List<OrderItemRequest> items;

    @Valid
    private PrescriptionRequest prescription;
}
