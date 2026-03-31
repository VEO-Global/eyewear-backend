package com.veo.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StaffOrderCustomerProfileResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
}
