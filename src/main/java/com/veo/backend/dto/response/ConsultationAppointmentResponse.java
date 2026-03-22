package com.veo.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConsultationAppointmentResponse {
    private Long id;
    private String phoneNumber;
    private LocalDateTime appointmentTime;
    private String status;
}
