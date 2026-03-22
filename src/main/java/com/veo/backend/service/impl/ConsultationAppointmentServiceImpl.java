package com.veo.backend.service.impl;

import com.veo.backend.dto.request.CreateConsultationAppointmentRequest;
import com.veo.backend.dto.response.ConsultationAppointmentResponse;
import com.veo.backend.entity.ConsultationAppointment;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.repository.ConsultationAppointmentRepository;
import com.veo.backend.service.ConsultationAppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultationAppointmentServiceImpl implements ConsultationAppointmentService {
    private final ConsultationAppointmentRepository appointmentRepository;

    @Override
    @Transactional
    public ConsultationAppointmentResponse createPublicAppointment(CreateConsultationAppointmentRequest request) {
        ConsultationAppointment appointment = ConsultationAppointment.builder()
                .phoneNumber(normalizePhoneNumber(request.getPhoneNumber()))
                .appointmentTime(request.getAppointmentTime())
                .status("pending")
                .build();

        appointmentRepository.save(appointment);
        return mapToResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultationAppointmentResponse> getStaffAppointments() {
        return appointmentRepository.findAllByOrderByAppointmentTimeAscIdDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationAppointmentResponse getAppointmentById(Long id) {
        ConsultationAppointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND, "Appointment not found"));

        return mapToResponse(appointment);
    }

    private ConsultationAppointmentResponse mapToResponse(ConsultationAppointment appointment) {
        return ConsultationAppointmentResponse.builder()
                .id(appointment.getId())
                .phoneNumber(appointment.getPhoneNumber())
                .appointmentTime(appointment.getAppointmentTime())
                .status(appointment.getStatus())
                .build();
    }

    private String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber == null ? null : phoneNumber.trim().replace(" ", "");
    }
}
