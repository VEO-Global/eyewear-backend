package com.veo.backend.dto.response;

import com.veo.backend.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentQrResponse {
    private Long orderId;
    private String orderCode;
    private PaymentStatus paymentStatus;
    private String qrCodeUrl;
    private String qrRawData;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountName;
    private String transferContent;
    private BigDecimal amountToPay;
    private LocalDateTime expiredAt;
}
