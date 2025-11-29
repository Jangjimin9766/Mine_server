package com.mine.api.controller;

import com.mine.api.dto.MagazineCreateRequest;
import com.mine.api.service.MagazineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalApiController.class)
class InternalApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MagazineService magazineService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("매거진 생성 (Internal) 성공")
    @WithMockUser
    void createMagazine_Success() throws Exception {
        // Given
        MagazineCreateRequest request = new MagazineCreateRequest();
        request.setTitle("Test Magazine");
        request.setUserEmail("test@example.com");

        given(magazineService.saveMagazine(any(MagazineCreateRequest.class), anyString())).willReturn(1L);

        // When & Then
        mockMvc.perform(post("/api/internal/magazine")
                .with(csrf())
                .header("X-Internal-Key", "mine-secret-key-1234")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }
}
