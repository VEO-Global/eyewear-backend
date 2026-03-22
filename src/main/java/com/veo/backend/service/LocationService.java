package com.veo.backend.service;

import com.veo.backend.dto.response.LocationItemResponse;

import java.util.List;

public interface LocationService {
    List<LocationItemResponse> getProvinces();

    List<LocationItemResponse> getDistricts(String provinceCode);

    List<LocationItemResponse> getWards(String districtCode);
}
