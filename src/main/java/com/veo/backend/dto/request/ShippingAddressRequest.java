package com.veo.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShippingAddressRequest {
    private String cityCode;
    private String cityName;
    private String districtCode;
    private String districtName;
    private String wardCode;
    private String wardName;

    @NotBlank(message = "Address detail is required")
    private String addressDetail;
}
