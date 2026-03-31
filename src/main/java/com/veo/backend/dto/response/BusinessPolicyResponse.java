package com.veo.backend.dto.response;

import com.veo.backend.enums.BusinessPolicyType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BusinessPolicyResponse {
    private BusinessPolicyType type;
    private String key;
    private String title;
    private String content;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
