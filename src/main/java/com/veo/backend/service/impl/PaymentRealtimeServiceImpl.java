package com.veo.backend.service.impl;

import com.veo.backend.dto.response.PaymentRealtimeEventResponse;
import com.veo.backend.service.PaymentRealtimeService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class PaymentRealtimeServiceImpl implements PaymentRealtimeService {
    private static final long SSE_TIMEOUT_MS = 0L;

    private final Map<String, List<SseEmitter>> customerEmitters = new ConcurrentHashMap<>();
    private final Map<String, List<SseEmitter>> staffEmitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribeCustomer(String email) {
        return registerEmitter(customerEmitters, email, "connected", "Subscribed to customer payment updates");
    }

    @Override
    public SseEmitter subscribeStaff(String email) {
        return registerEmitter(staffEmitters, email, "connected", "Subscribed to staff payment updates");
    }

    @Override
    public void publishToCustomer(String email, String eventName, PaymentRealtimeEventResponse payload) {
        if (email == null || email.isBlank()) {
            return;
        }
        sendToEmitters(customerEmitters.get(email), eventName, payload);
    }

    @Override
    public void publishToStaff(String eventName, PaymentRealtimeEventResponse payload) {
        for (List<SseEmitter> emitters : staffEmitters.values()) {
            sendToEmitters(emitters, eventName, payload);
        }
    }

    private SseEmitter registerEmitter(
            Map<String, List<SseEmitter>> emitterGroups,
            String key,
            String initEventName,
            String initMessage
    ) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitterGroups.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(emitterGroups, key, emitter));
        emitter.onTimeout(() -> removeEmitter(emitterGroups, key, emitter));
        emitter.onError(ex -> removeEmitter(emitterGroups, key, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name(initEventName)
                    .data(PaymentRealtimeEventResponse.builder()
                            .eventType(initEventName)
                            .message(initMessage)
                            .build()));
        } catch (IOException ex) {
            removeEmitter(emitterGroups, key, emitter);
        }

        return emitter;
    }

    private void sendToEmitters(List<SseEmitter> emitters, String eventName, PaymentRealtimeEventResponse payload) {
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : List.copyOf(emitters)) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException ex) {
                emitters.remove(emitter);
                emitter.complete();
            }
        }
    }

    private void removeEmitter(Map<String, List<SseEmitter>> emitterGroups, String key, SseEmitter emitter) {
        List<SseEmitter> emitters = emitterGroups.get(key);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emitterGroups.remove(key);
        }
    }
}
