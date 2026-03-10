package com.veo.backend.dto.request;

import com.veo.backend.entity.Prescription;
import com.veo.backend.enums.OrderType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderCreateRequest {
    private OrderType orderType;
    private String shippingAddress;
    private String phoneNumber;
    private String receiverName;
    private String note;
    private List<OrderItemRequest> items;
    private PrescriptionRequest prescription;
}
