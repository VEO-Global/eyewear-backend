package com.veo.backend.service.impl;

import com.veo.backend.enums.OrderStatus;

final class OrderCustomerTabResolver {
    private OrderCustomerTabResolver() {
    }

    static String resolve(OrderStatus status) {
        if (status == null) {
            return "tat-ca";
        }

        return switch (status) {
            case PENDING_VERIFICATION -> "cho-gia-cong";
            case MANUFACTURING, PACKING -> "cho-gia-cong";
            case READY_TO_SHIP -> "van-chuyen";
            case PENDING_PAYMENT, WAITING_FOR_STOCK, SHIPPING -> "cho-giao-hang";
            case COMPLETED -> "hoan-thanh";
            case CANCELLED -> "da-huy";
        };
    }
}
