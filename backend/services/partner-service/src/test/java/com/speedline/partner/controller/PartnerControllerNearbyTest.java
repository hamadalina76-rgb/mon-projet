package com.speedline.partner.controller;

import com.speedline.partner.dto.PartnerDTO;
import com.speedline.partner.dto.request.PartnerFilterRequest;
import com.speedline.partner.repository.PartnerRepository;
import com.speedline.partner.service.FileStorageService;
import com.speedline.partner.service.PartnerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PartnerController.class)
@AutoConfigureMockMvc(addFilters = false)
class PartnerControllerNearbyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PartnerService partnerService;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private PartnerRepository partnerRepository;

    @Test
    void tc16_isOpenTrue_shouldForwardOpenFilter() throws Exception {
        when(partnerService.getNearbyPartners(any(PartnerFilterRequest.class))).thenReturn(singlePartnerPage());

        mockMvc.perform(get("/partners/nearby")
                        .param("lat", "36.8")
                        .param("lng", "10.1")
                        .param("isOpen", "true"))
                .andExpect(status().isOk());

        ArgumentCaptor<PartnerFilterRequest> captor = ArgumentCaptor.forClass(PartnerFilterRequest.class);
        verify(partnerService).getNearbyPartners(captor.capture());
        assertThat(captor.getValue().getIsOpen()).isTrue();
    }

    @Test
    void tc17_categoryId_shouldForwardCategoryFilter() throws Exception {
        when(partnerService.getNearbyPartners(any(PartnerFilterRequest.class))).thenReturn(singlePartnerPage());

        mockMvc.perform(get("/partners/nearby")
                        .param("lat", "36.8")
                        .param("lng", "10.1")
                        .param("categoryId", "12"))
                .andExpect(status().isOk());

        ArgumentCaptor<PartnerFilterRequest> captor = ArgumentCaptor.forClass(PartnerFilterRequest.class);
        verify(partnerService).getNearbyPartners(captor.capture());
        assertThat(captor.getValue().getCategoryId()).isEqualTo("12");
    }

    @Test
    void tc18_minRating_shouldForwardMinRatingFilter() throws Exception {
        when(partnerService.getNearbyPartners(any(PartnerFilterRequest.class))).thenReturn(singlePartnerPage());

        mockMvc.perform(get("/partners/nearby")
                        .param("lat", "36.8")
                        .param("lng", "10.1")
                        .param("minRating", "4.0"))
                .andExpect(status().isOk());

        ArgumentCaptor<PartnerFilterRequest> captor = ArgumentCaptor.forClass(PartnerFilterRequest.class);
        verify(partnerService).getNearbyPartners(captor.capture());
        assertThat(captor.getValue().getMinRating()).isEqualTo(4.0d);
    }

    @Test
    void tc19_freeDeliveryTrue_shouldForwardFreeDeliveryFilter() throws Exception {
        when(partnerService.getNearbyPartners(any(PartnerFilterRequest.class))).thenReturn(singlePartnerPage());

        mockMvc.perform(get("/partners/nearby")
                        .param("lat", "36.8")
                        .param("lng", "10.1")
                        .param("freeDelivery", "true"))
                .andExpect(status().isOk());

        ArgumentCaptor<PartnerFilterRequest> captor = ArgumentCaptor.forClass(PartnerFilterRequest.class);
        verify(partnerService).getNearbyPartners(captor.capture());
        assertThat(captor.getValue().getFreeDelivery()).isTrue();
    }

    @Test
    void tc20_sortByRating_shouldReturnHighestRatingFirst() throws Exception {
        PartnerDTO p1 = PartnerDTO.builder().id(1L).businessName("A").rating(new BigDecimal("4.9")).build();
        PartnerDTO p2 = PartnerDTO.builder().id(2L).businessName("B").rating(new BigDecimal("4.2")).build();
        Page<PartnerDTO> page = new PageImpl<>(List.of(p1, p2), PageRequest.of(0, 20), 2);
        when(partnerService.getNearbyPartners(any(PartnerFilterRequest.class))).thenReturn(page);

        mockMvc.perform(get("/partners/nearby")
                        .param("lat", "36.8")
                        .param("lng", "10.1")
                        .param("sortBy", "rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating").value(4.9));

        ArgumentCaptor<PartnerFilterRequest> captor = ArgumentCaptor.forClass(PartnerFilterRequest.class);
        verify(partnerService).getNearbyPartners(captor.capture());
        assertThat(captor.getValue().getSortBy()).isEqualTo("rating");
    }

    @Test
    void tc21_intersection_shouldForwardAllCombinedFilters() throws Exception {
        when(partnerService.getNearbyPartners(any(PartnerFilterRequest.class))).thenReturn(singlePartnerPage());

        mockMvc.perform(get("/partners/nearby")
                        .param("lat", "36.8")
                        .param("lng", "10.1")
                        .param("isOpen", "true")
                        .param("freeDelivery", "true")
                        .param("minRating", "4.0"))
                .andExpect(status().isOk());

        ArgumentCaptor<PartnerFilterRequest> captor = ArgumentCaptor.forClass(PartnerFilterRequest.class);
        verify(partnerService).getNearbyPartners(captor.capture());
        PartnerFilterRequest req = captor.getValue();

        assertThat(req.getIsOpen()).isTrue();
        assertThat(req.getFreeDelivery()).isTrue();
        assertThat(req.getMinRating()).isEqualTo(4.0d);
    }

    @Test
    void tc22_minRatingFive_shouldReturnHttp200WithEmptyContent() throws Exception {
        Page<PartnerDTO> empty = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(partnerService.getNearbyPartners(any(PartnerFilterRequest.class))).thenReturn(empty);

        mockMvc.perform(get("/partners/nearby")
                        .param("lat", "36.8")
                        .param("lng", "10.1")
                        .param("minRating", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    private Page<PartnerDTO> singlePartnerPage() {
        PartnerDTO partner = PartnerDTO.builder()
                .id(1L)
                .businessName("Partner A")
                .rating(new BigDecimal("4.4"))
                .build();
        return new PageImpl<>(List.of(partner), PageRequest.of(0, 20), 1);
    }
}
