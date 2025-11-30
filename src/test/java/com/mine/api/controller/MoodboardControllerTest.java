package com.mine.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mine.api.dto.MoodboardRequestDto;
import com.mine.api.service.MoodboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MoodboardController.class)
class MoodboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MoodboardService moodboardService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "testUser")
    void createMoodboard_Success() throws Exception {
        // Given
        MoodboardRequestDto request = MoodboardRequestDto.builder()
                .topic("Cozy Office")
                .user_mood("Relaxed")
                .user_interests(List.of("Desk", "Tech"))
                .build();

        String expectedUrl = "https://s3.ap-northeast-2.amazonaws.com/bucket/image.png";
        given(moodboardService.createMoodboard(eq("testUser"), any(MoodboardRequestDto.class)))
                .willReturn(expectedUrl);

        // When & Then
        mockMvc.perform(post("/api/moodboards")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.image_url").value(expectedUrl))
                .andExpect(jsonPath("$.description").exists());
    }
}
