package com.veo.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserAddressResponse {
    private Long id;
    private String addressLine;
    private String addressDetail;
    private String ward;
    private String district;
    private String province;
    private Boolean isDefault;
}
