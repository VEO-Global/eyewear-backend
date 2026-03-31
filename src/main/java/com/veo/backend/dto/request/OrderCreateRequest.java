package com.veo.backend.dto.request;

import com.veo.backend.enums.PaymentMethod;
import com.veo.backend.enums.PrescriptionOption;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateRequest {
    private PaymentMethod paymentMethod;

    @NotNull(message = "Prescription option is required")
    private PrescriptionOption prescriptionOption;

    @Valid
    private ShippingAddressRequest shippingAddress;

    private String city;

    private String district;

    private String ward;

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
