package com.veo.backend.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateConsultationAppointmentRequest {
    private static final String VN_PHONE_REGEX = "^(\\+84|0)(3|5|7|8|9)\\d{8}$";

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = VN_PHONE_REGEX, message = "Phone number must be a valid Vietnamese mobile number")
    private String phoneNumber;

    @NotNull(message = "Appointment time is required")
    @FutureOrPresent(message = "Appointment time must not be in the past")
    private LocalDateTime appointmentTime;
}
