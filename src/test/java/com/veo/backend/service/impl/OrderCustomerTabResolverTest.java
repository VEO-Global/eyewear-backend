package com.veo.backend.service.impl;

import com.veo.backend.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderCustomerTabResolverTest {

    @Test
    void shouldMapManufacturingToProcessingTabOnly() {
        assertEquals("cho-gia-cong", OrderCustomerTabResolver.resolve(OrderStatus.MANUFACTURING));
        assertEquals("cho-gia-cong", OrderCustomerTabResolver.resolve(OrderStatus.PACKING));
    }

    @Test
    void shouldMapReadyToShipToShippingTab() {
        assertEquals("van-chuyen", OrderCustomerTabResolver.resolve(OrderStatus.READY_TO_SHIP));
    }

    @Test
    void shouldMapShippingToWaitingDeliveryTab() {
        assertEquals("cho-giao-hang", OrderCustomerTabResolver.resolve(OrderStatus.SHIPPING));
    }

    @Test
    void shouldMapCompletedAndCancelledToFinalTabs() {
        assertEquals("hoan-thanh", OrderCustomerTabResolver.resolve(OrderStatus.COMPLETED));
        assertEquals("da-huy", OrderCustomerTabResolver.resolve(OrderStatus.CANCELLED));
    }
}
