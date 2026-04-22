package com.speedline.delivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speedline.delivery.dispatch.dto.DispatchDashboardKpisResponse;
import com.speedline.delivery.dispatch.dto.ZoneStatusRequest;
import com.speedline.delivery.dispatch.service.DispatchAdminActionService;
import com.speedline.delivery.dispatch.service.DispatchDashboardQueryService;
import com.speedline.delivery.dispatch.service.DispatchProposalService;
import com.speedline.delivery.dispatch.service.DispatchRealtimePublisher;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DispatchDashboardController.class)
class DispatchDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DispatchDashboardQueryService queryService;
    @MockBean
    private DispatchAdminActionService adminActionService;
    @MockBean
    private DispatchProposalService dispatchProposalService;
    @MockBean
    private DispatchRealtimePublisher realtimePublisher;

    @Test
    void returnsKpis() throws Exception {
        Mockito.when(queryService.getKpis()).thenReturn(DispatchDashboardKpisResponse.builder()
                .firstCycleDispatchRate(86.0)
                .averageAssignmentDelaySeconds(70.0)
                .deliveriesPerCourierPerHour(3.2)
                .bundlingRate(42.0)
                .failureRate(1.5)
                .onTimeRate(93.0)
                .build());

        mockMvc.perform(get("/dispatch/dashboard/kpis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstCycleDispatchRate").value(86.0))
                .andExpect(jsonPath("$.onTimeRate").value(93.0));
    }

    @Test
    void updatesZoneStatus() throws Exception {
        Mockito.when(adminActionService.updateZoneStatus(9L, false)).thenReturn(Map.of("id", 9, "isActive", false));
        ZoneStatusRequest request = new ZoneStatusRequest();
        request.setActive(false);

        mockMvc.perform(put("/dispatch/zones/9/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void returnsCourierPositions() throws Exception {
        Mockito.when(queryService.getCourierPositions()).thenReturn(List.of());
        mockMvc.perform(get("/dispatch/couriers/positions"))
                .andExpect(status().isOk());
    }
}
