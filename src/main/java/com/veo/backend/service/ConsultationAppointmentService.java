package com.veo.backend.service;

import com.veo.backend.dto.request.CreateConsultationAppointmentRequest;
import com.veo.backend.dto.response.ConsultationAppointmentResponse;
import java.util.List;

public interface ConsultationAppointmentService {
    ConsultationAppointmentResponse createPublicAppointment(CreateConsultationAppointmentRequest request);

    List<ConsultationAppointmentResponse> getStaffAppointments();

    ConsultationAppointmentResponse getAppointmentById(Long id);
}
