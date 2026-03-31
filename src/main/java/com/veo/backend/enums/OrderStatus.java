package com.veo.backend.enums;

public enum OrderStatus {
    PENDING_PAYMENT,
    PENDING_VERIFICATION,
    WAITING_FOR_STOCK,
    MANUFACTURING,
    PACKING,
    READY_TO_SHIP,
    SHIPPING,
    COMPLETED,
    CANCELLED,
}
