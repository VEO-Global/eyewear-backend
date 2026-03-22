package com.veo.backend.controller;

import com.veo.backend.dto.response.LocationItemResponse;
import com.veo.backend.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LocationControllerTest {
    private LocationService locationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        locationService = mock(LocationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new LocationController(locationService)).build();
    }

    @Test
    void getProvincesShouldReturnCodeAndNameArray() throws Exception {
        when(locationService.getProvinces()).thenReturn(List.of(
                LocationItemResponse.builder().code(1).name("Thành phố Hà Nội").build(),
                LocationItemResponse.builder().code(79).name("Thành phố Hồ Chí Minh").build()
        ));

        mockMvc.perform(get("/api/locations/provinces"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].code").value(1))
                .andExpect(jsonPath("$[0].name").value("Thành phố Hà Nội"))
                .andExpect(jsonPath("$[1].code").value(79))
                .andExpect(jsonPath("$[1].name").value("Thành phố Hồ Chí Minh"));
    }

    @Test
    void getDistrictsShouldReadProvinceCodeParam() throws Exception {
        when(locationService.getDistricts("79")).thenReturn(List.of(
                LocationItemResponse.builder().code(760).name("Quận 1").build()
        ));

        mockMvc.perform(get("/api/locations/districts").param("provinceCode", "79"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value(760))
                .andExpect(jsonPath("$[0].name").value("Quận 1"));
    }

    @Test
    void getWardsShouldReadDistrictCodeParam() throws Exception {
        when(locationService.getWards("760")).thenReturn(List.of(
                LocationItemResponse.builder().code(26734).name("Phường Bến Nghé").build()
        ));

        mockMvc.perform(get("/api/locations/wards").param("districtCode", "760"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value(26734))
                .andExpect(jsonPath("$[0].name").value("Phường Bến Nghé"));
    }
}
