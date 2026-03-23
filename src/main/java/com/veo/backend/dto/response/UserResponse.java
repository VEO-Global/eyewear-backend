package com.veo.backend.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String addressLine;
    private String addressDetail;
    private String ward;
    private String district;
    private String city;
    private String avatarUrl;
    private Boolean isActive;
    private String role;
}
