package com.veo.backend.controller;

import com.veo.backend.dto.response.StaffOrderResponse;
import com.veo.backend.enums.StaffOrderPhase;
import com.veo.backend.service.StaffOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StaffOrderControllerTest {

    private MockMvc mockMvc;
    private StaffOrderService staffOrderService;

    @BeforeEach
    void setUp() {
        staffOrderService = mock(StaffOrderService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new StaffOrderController(staffOrderService)).build();
    }

    @Test
    void handoffOrder_shouldAcceptPatchWithoutRequestBody() throws Exception {
        when(staffOrderService.handoffOrder(138L, "staff@veo.com")).thenReturn(
                StaffOrderResponse.builder()
                        .id(138L)
                        .orderNumber("ORD-138")
                        .status("PACKING")
                        .phase(StaffOrderPhase.PROCESSING)
                        .phaseLabel("Processing")
                        .build()
        );

        mockMvc.perform(
                        patch("/api/staff/orders/138/handoff")
                                .principal(new TestingAuthenticationToken("staff@veo.com", null))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(138))
                .andExpect(jsonPath("$.orderNumber").value("ORD-138"))
                .andExpect(jsonPath("$.status").value("PACKING"))
                .andExpect(jsonPath("$.phase").value("PROCESSING"));
    }
}
