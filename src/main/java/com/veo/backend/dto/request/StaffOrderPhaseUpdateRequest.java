package com.veo.backend.dto.request;

import com.veo.backend.enums.StaffOrderPhase;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StaffOrderPhaseUpdateRequest {
    @NotNull(message = "Phase is required")
    private StaffOrderPhase phase;

    private String note;
}
