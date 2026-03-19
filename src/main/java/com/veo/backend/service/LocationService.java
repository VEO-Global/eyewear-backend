package com.veo.backend.service;

import java.util.List;

public interface LocationService {
    List<String> getProvinces();

    List<String> getDistricts(String provinceCode);

    List<String> getWards(String districtCode);
}
