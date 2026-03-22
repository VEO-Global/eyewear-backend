package com.veo.backend.controller;

import com.veo.backend.dto.request.CreateConsultationAppointmentRequest;
import com.veo.backend.dto.response.ConsultationAppointmentResponse;
import com.veo.backend.service.ConsultationAppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/consultation-appointments")
@RequiredArgsConstructor
public class ConsultationAppointmentController {
    private final ConsultationAppointmentService consultationAppointmentService;

    @PostMapping
    public ResponseEntity<ConsultationAppointmentResponse> createAppointment(
            @Valid @RequestBody CreateConsultationAppointmentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(consultationAppointmentService.createPublicAppointment(request));
    }
}
