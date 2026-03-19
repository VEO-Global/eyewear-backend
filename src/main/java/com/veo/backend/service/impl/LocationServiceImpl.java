package com.veo.backend.service.impl;

import com.veo.backend.service.LocationService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LocationServiceImpl implements LocationService {
    private final RestTemplate restTemplate;

    public LocationServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<String> getProvinces() {
        String url = "https://provinces.open-api.vn/api/p";
        List<Map> provinces = restTemplate.getForObject(url, List.class);
        List<String> names = new ArrayList<>();
        if (provinces != null) {
            for (Map p : provinces) {
                Object name = p.get("name");
                if (name != null) names.add(name.toString());
            }
        }
        return names;
    }

    @Override
    public List<String> getDistricts(String provinceCode) {
        String url = String.format("https://provinces.open-api.vn/api/p/%s?depth=2", provinceCode);
        Map province = restTemplate.getForObject(url, Map.class);
        List<String> names = new ArrayList<>();
        if (province != null && province.get("districts") instanceof List) {
            for (Object d : (List) province.get("districts")) {
                if (d instanceof Map) {
                    Object name = ((Map) d).get("name");
                    if (name != null) names.add(name.toString());
                }
            }
        }
        return names;
    }

    @Override
    public List<String> getWards(String districtCode) {
        String url = String.format("https://provinces.open-api.vn/api/d/%s?depth=2", districtCode);
        Map district = restTemplate.getForObject(url, Map.class);
        List<String> names = new ArrayList<>();
        if (district != null && district.get("wards") instanceof List) {
            for (Object w : (List) district.get("wards")) {
                if (w instanceof Map) {
                    Object name = ((Map) w).get("name");
                    if (name != null) names.add(name.toString());
                }
            }
        }
        return names;
    }
}
