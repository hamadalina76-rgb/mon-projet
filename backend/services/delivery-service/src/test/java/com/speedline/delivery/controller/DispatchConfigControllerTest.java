package com.speedline.delivery.controller;

import com.speedline.delivery.dispatch.config.api.dto.DispatchConfigDtos;
import com.speedline.delivery.dispatch.config.service.DispatchConfigAuditCsvService;
import com.speedline.delivery.dispatch.config.service.DispatchConfigManagementService;
import com.speedline.delivery.dispatch.config.service.DispatchReplayService;
import com.speedline.delivery.dispatch.config.service.DispatchSimulationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DispatchConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
class DispatchConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DispatchConfigManagementService managementService;
    @MockBean
    private DispatchSimulationService simulationService;
    @MockBean
    private DispatchReplayService replayService;
    @MockBean
    private DispatchConfigAuditCsvService auditCsvService;

    @Test
    void getGeneralReturnsEnvelope() throws Exception {
        DispatchConfigDtos.MetaResponse meta = new DispatchConfigDtos.MetaResponse();
        meta.setActiveVersion(2L);
        meta.setOptimisticLock(3);
        meta.setRedisConfigVersion(2L);
        DispatchConfigDtos.GeneralConfigDto data = new DispatchConfigDtos.GeneralConfigDto();
        data.setIntervalSeconds(7);
        DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.GeneralConfigDto> env = new DispatchConfigDtos.GroupEnvelope<>();
        env.setMeta(meta);
        env.setData(data);
        Mockito.when(managementService.getGeneral()).thenReturn(env);

        mockMvc.perform(get("/dispatch/dispatch-config/general").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.activeVersion").value(2))
                .andExpect(jsonPath("$.data.intervalSeconds").value(7));
    }

    @Test
    void putScoringReturnsEnvelope() throws Exception {
        DispatchConfigDtos.MetaResponse meta = new DispatchConfigDtos.MetaResponse();
        meta.setActiveVersion(3L);
        meta.setOptimisticLock(4);
        meta.setRedisConfigVersion(3L);
        DispatchConfigDtos.ScoringConfigDto scoring = new DispatchConfigDtos.ScoringConfigDto();
        DispatchConfigDtos.ScoringComponentDto row = new DispatchConfigDtos.ScoringComponentDto();
        row.setKey(com.speedline.delivery.matching.cost.model.CostComponentKey.AVAILABILITY);
        row.setEnabled(true);
        row.setWeight(8);
        row.setOrder(3);
        scoring.setComponents(List.of(row));
        DispatchConfigDtos.GroupEnvelope<DispatchConfigDtos.ScoringConfigDto> env = new DispatchConfigDtos.GroupEnvelope<>();
        env.setMeta(meta);
        env.setData(scoring);
        Mockito.when(managementService.putScoring(Mockito.any(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(env);

        String body = """
                {"baseVersion":2,"data":{"components":[{"key":"AVAILABILITY","enabled":true,"weight":8,"order":3}]}}
                """;
        mockMvc.perform(put("/dispatch/dispatch-config/scoring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Role", "ADMIN")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.activeVersion").value(3));
    }
}
