package com.speedline.partner.controller;

import com.speedline.partner.dto.response.FavoriteResponse;
import com.speedline.partner.dto.response.FavoriteUpsertResult;
import com.speedline.partner.exception.PartnerNotFoundException;
import com.speedline.partner.exception.ResourceNotFoundException;
import com.speedline.partner.repository.PartnerRepository;
import com.speedline.partner.service.FavoriteService;
import com.speedline.partner.service.FileStorageService;
import com.speedline.partner.service.PartnerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PartnerController.class)
@AutoConfigureMockMvc(addFilters = false)
class PartnerControllerFavoritesTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PartnerService partnerService;

    @MockBean
    private FavoriteService favoriteService;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private PartnerRepository partnerRepository;

    @Test
    void tcBe01_postExistingFavorite_shouldReturn200AndNoDuplicate() throws Exception {
        FavoriteResponse existing = FavoriteResponse.builder()
            .id(17L)
            .customerId(42L)
            .partnerId(9L)
            .createdAt(LocalDateTime.now())
            .build();

        when(favoriteService.addFavorite(42L, 9L))
            .thenReturn(new FavoriteUpsertResult(existing, false));

        mockMvc.perform(post("/partners/favorites")
                .header("X-User-Id", "42")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"partnerId\":\"9\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(17))
            .andExpect(jsonPath("$.customerId").value(42))
            .andExpect(jsonPath("$.partnerId").value(9));

        verify(favoriteService, times(1)).addFavorite(42L, 9L);
    }

    @Test
    void tcBe02_deleteNonExistingFavorite_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Favorite not found for partnerId: 99"))
            .when(favoriteService)
            .removeFavorite(42L, 99L);

        mockMvc.perform(delete("/partners/favorites/99")
                .header("X-User-Id", "42"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Favorite not found for partnerId: 99"));
    }

    @Test
    void tcBe03_getWithoutJwt_shouldReturn401() throws Exception {
        mockMvc.perform(get("/partners/favorites")
                .param("userId", "42"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void tcBe04_postWithUnknownPartner_shouldReturn404WithExplicitMessage() throws Exception {
        when(favoriteService.addFavorite(42L, 999L))
            .thenThrow(new PartnerNotFoundException("Partner not found with id: 999"));

        mockMvc.perform(post("/partners/favorites")
                .header("X-User-Id", "42")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"partnerId\":\"999\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Partner not found with id: 999"));
    }
}
