package com.veo.backend.service;

import com.veo.backend.dto.response.PaymentRealtimeEventResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface PaymentRealtimeService {
    SseEmitter subscribeCustomer(String email);

    SseEmitter subscribeStaff(String email);

    void publishToCustomer(String email, String eventName, PaymentRealtimeEventResponse payload);

    void publishToStaff(String eventName, PaymentRealtimeEventResponse payload);
}
