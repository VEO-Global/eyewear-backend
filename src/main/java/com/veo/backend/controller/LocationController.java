package com.veo.backend.controller;

import com.veo.backend.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {
    private final LocationService locationService;

    @GetMapping("/provinces")
    public List<String> getProvinces() {
        return locationService.getProvinces();
    }

    @GetMapping("/districts")
    public List<String> getDistricts(@RequestParam String provinceCode) {
        return locationService.getDistricts(provinceCode);
    }

    @GetMapping("/wards")
    public List<String> getWards(@RequestParam String districtCode) {
        return locationService.getWards(districtCode);
    }
}
