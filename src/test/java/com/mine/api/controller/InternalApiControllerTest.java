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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = InternalApiController.class, properties = "mine.internal.secret-key=test-internal-key")
class InternalApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MagazineService magazineService;

    @MockBean
    private com.mine.api.service.S3Service s3Service;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("매거진 생성 (Internal) 성공")
    @WithMockUser
    void createMagazine_Success() throws Exception {
        // Given
        MagazineCreateRequest request = new MagazineCreateRequest();
        request.setTitle("Test Magazine");
        request.setIntroduction("Safe introduction");
        request.setUserEmail("test@example.com");

        MagazineCreateRequest.ParagraphDto paragraph = new MagazineCreateRequest.ParagraphDto();
        paragraph.setText("Safe paragraph");

        MagazineCreateRequest.SectionDto section = new MagazineCreateRequest.SectionDto();
        section.setHeading("Section 1");
        section.setParagraphs(java.util.List.of(paragraph));
        request.setSections(java.util.List.of(section));

        given(magazineService.saveMagazine(any(MagazineCreateRequest.class), anyString())).willReturn(1L);

        // When & Then
        mockMvc.perform(post("/api/internal/magazine")
                .with(csrf())
                .header("X-Internal-Key", "test-internal-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    @DisplayName("매거진 생성 (Internal) - 필수 구조 누락 시 400")
    @WithMockUser
    void createMagazine_InvalidRequest_ReturnsBadRequest() throws Exception {
        MagazineCreateRequest request = new MagazineCreateRequest();
        request.setUserEmail("test@example.com");

        mockMvc.perform(post("/api/internal/magazine")
                .with(csrf())
                .header("X-Internal-Key", "mine-secret-key-1234")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("title is required")))
                .andExpect(jsonPath("$.message", containsString("introduction is required")))
                .andExpect(jsonPath("$.message", containsString("sections must not be empty")));

        verifyNoInteractions(magazineService);
    }
}
