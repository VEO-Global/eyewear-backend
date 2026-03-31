package com.veo.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ManagerUserStatusRequest {
    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
