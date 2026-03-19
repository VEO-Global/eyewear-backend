package com.veo.backend.dto.request;

import lombok.Data;

@Data
public class UserCreateRequest {
    private String email;
    private String password;
    private String fullName;
    private String roleName;
}
