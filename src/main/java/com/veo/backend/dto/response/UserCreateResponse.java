package com.veo.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserCreateResponse {
    private Long id;

    private String email;

    private String fullName;

    private String role;

    private String phone;

    private String avatarUrl;

    private Boolean isActive;
}
