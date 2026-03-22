package com.veo.backend.controller;

import com.veo.backend.dto.response.ConsultationAppointmentResponse;
import com.veo.backend.service.ConsultationAppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/consultation-appointments")
@RequiredArgsConstructor
public class StaffConsultationAppointmentController {
    private final ConsultationAppointmentService consultationAppointmentService;

    @GetMapping
    public List<ConsultationAppointmentResponse> getAppointments() {
        return consultationAppointmentService.getStaffAppointments();
    }

    @GetMapping("/{id}")
    public ConsultationAppointmentResponse getById(@PathVariable Long id) {
        return consultationAppointmentService.getAppointmentById(id);
    }
}
