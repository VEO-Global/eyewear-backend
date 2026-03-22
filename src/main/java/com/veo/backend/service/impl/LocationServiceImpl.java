package com.veo.backend.service.impl;

import com.veo.backend.dto.response.LocationItemResponse;
import com.veo.backend.exception.AppException;
import com.veo.backend.exception.ErrorCode;
import com.veo.backend.service.LocationService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LocationServiceImpl implements LocationService {
    private static final String PROVINCES_API_BASE_URL = "https://provinces.open-api.vn/api";
    private final RestTemplate restTemplate;

    public LocationServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<LocationItemResponse> getProvinces() {
        List<Map<String, Object>> provinces = fetchList(PROVINCES_API_BASE_URL + "/p/");
        return mapLocationItems(provinces, "province");
    }

    @Override
    public List<LocationItemResponse> getDistricts(String provinceCode) {
        String normalizedProvinceCode = requireCode(provinceCode, "Province code is required");
        Map<String, Object> province = fetchObject(PROVINCES_API_BASE_URL + "/p/" + normalizedProvinceCode + "?depth=2");
        Object districts = province.get("districts");
        return mapLocationItems(districts, "district");
    }

    @Override
    public List<LocationItemResponse> getWards(String districtCode) {
        String normalizedDistrictCode = requireCode(districtCode, "District code is required");
        Map<String, Object> district = fetchObject(PROVINCES_API_BASE_URL + "/d/" + normalizedDistrictCode + "?depth=2");
        Object wards = district.get("wards");
        return mapLocationItems(wards, "ward");
    }

    private String requireCode(String code, String message) {
        if (code == null || code.trim().isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, message);
        }
        return code.trim();
    }

    private List<Map<String, Object>> fetchList(String url) {
        try {
            List<Map<String, Object>> result = restTemplate.getForObject(url, List.class);
            return result == null ? List.of() : result;
        } catch (RestClientException ex) {
            throw new AppException(ErrorCode.LOCATION_API_ERROR, "Cannot load locations from open-api");
        }
    }

    private Map<String, Object> fetchObject(String url) {
        try {
            Map<String, Object> result = restTemplate.getForObject(url, Map.class);
            if (result == null || result.isEmpty()) {
                throw new AppException(ErrorCode.LOCATION_API_ERROR, "Location data is empty");
            }
            return result;
        } catch (AppException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new AppException(ErrorCode.LOCATION_API_ERROR, "Cannot load locations from open-api");
        }
    }

    private List<LocationItemResponse> mapLocationItems(Object rawItems, String locationType) {
        if (!(rawItems instanceof List<?> rawList)) {
            throw new AppException(ErrorCode.LOCATION_API_ERROR, "Location data is invalid");
        }

        List<LocationItemResponse> items = new ArrayList<>();

        for (Object rawItem : rawList) {
            if (!(rawItem instanceof Map<?, ?> itemMap)) {
                continue;
            }

            Integer code = toInteger(itemMap.get("code"));
            String name = itemMap.get("name") == null ? null : itemMap.get("name").toString();

            if (code != null && name != null && !name.isBlank()) {
                items.add(LocationItemResponse.builder()
                        .code(code)
                        .name(name)
                        .build());
            }
        }

        if (items.isEmpty()) {
            throw new AppException(ErrorCode.LOCATION_API_ERROR,
                    "Cannot load " + locationType + " data from open-api");
        }

        return items;
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

}
