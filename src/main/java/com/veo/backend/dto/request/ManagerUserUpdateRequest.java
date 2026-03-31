package com.veo.backend.dto.request;

import lombok.Data;

@Data
public class ManagerUserUpdateRequest {
    private String fullName;
    private String phone;
    private String avatarUrl;
    private String roleName;
    private Boolean isActive;
}
