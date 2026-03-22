package com.veo.backend.service.impl;

import com.veo.backend.dto.response.LocationItemResponse;
import com.veo.backend.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocationServiceImplTest {
    private RestTemplate restTemplate;
    private LocationServiceImpl locationService;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        locationService = new LocationServiceImpl(restTemplate);
    }

    @Test
    void getProvincesShouldMapCodeAndName() {
        when(restTemplate.getForObject("https://provinces.open-api.vn/api/p", List.class))
                .thenReturn(List.of(
                        Map.of("code", 1, "name", "Thành phố Hà Nội"),
                        Map.of("code", 79, "name", "Thành phố Hồ Chí Minh")
                ));

        List<LocationItemResponse> result = locationService.getProvinces();

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getCode());
        assertEquals("Thành phố Hà Nội", result.get(0).getName());
        assertEquals(79, result.get(1).getCode());
        assertEquals("Thành phố Hồ Chí Minh", result.get(1).getName());
    }

    @Test
    void getDistrictsShouldMapNestedDistricts() {
        when(restTemplate.getForObject("https://provinces.open-api.vn/api/p/79?depth=2", Map.class))
                .thenReturn(Map.of(
                        "districts", List.of(
                                Map.of("code", 760, "name", "Quận 1"),
                                Map.of("code", 769, "name", "Quận 7")
                        )
                ));

        List<LocationItemResponse> result = locationService.getDistricts("79");

        assertEquals(2, result.size());
        assertEquals(760, result.get(0).getCode());
        assertEquals("Quận 1", result.get(0).getName());
    }

    @Test
    void getWardsShouldMapNestedWards() {
        when(restTemplate.getForObject("https://provinces.open-api.vn/api/d/760?depth=2", Map.class))
                .thenReturn(Map.of(
                        "wards", List.of(
                                Map.of("code", 26734, "name", "Phường Bến Nghé"),
                                Map.of("code", 26737, "name", "Phường Bến Thành")
                        )
                ));

        List<LocationItemResponse> result = locationService.getWards("760");

        assertEquals(2, result.size());
        assertEquals(26734, result.get(0).getCode());
        assertEquals("Phường Bến Nghé", result.get(0).getName());
    }

    @Test
    void getProvincesShouldThrowWhenPayloadIsInvalid() {
        when(restTemplate.getForObject("https://provinces.open-api.vn/api/p", List.class))
                .thenReturn(List.of(Map.of("id", 1, "title", "invalid")));

        assertThrows(AppException.class, () -> locationService.getProvinces());
    }
}
