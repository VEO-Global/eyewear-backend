package com.veo.backend.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.veo.backend.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateNotificationRequest {
    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Notification message is required")
    private String message;

    @NotBlank(message = "Source module is required")
    @Size(max = 50, message = "Source module must not exceed 50 characters")
    private String sourceModule;

    private JsonNode metadata;
}
