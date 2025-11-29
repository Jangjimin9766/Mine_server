package com.mine.api.controller;

import com.mine.api.dto.InteractionDto;
import com.mine.api.service.MagazineInteractionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MagazineInteractionController.class)
class MagazineInteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MagazineInteractionService interactionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("매거진 상호작용 성공")
    @WithMockUser(username = "testuser")
    void interact_Success() throws Exception {
        // Given
        InteractionDto.InteractRequest request = new InteractionDto.InteractRequest();
        request.setMessage("Make it funnier");

        InteractionDto.InteractResponse response = new InteractionDto.InteractResponse();
        response.setMessage("Updated section");
        response.setActionType("regenerate_section");
        response.setMagazineId(1L);

        given(interactionService.interact(anyLong(), anyString(), any(InteractionDto.InteractRequest.class)))
                .willReturn(response);

        // When & Then
        mockMvc.perform(post("/api/magazines/1/interact")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Updated section"))
                .andExpect(jsonPath("$.actionType").value("regenerate_section"));
    }

    @Test
    @DisplayName("상호작용 이력 조회 성공")
    @WithMockUser(username = "testuser")
    void getHistory_Success() throws Exception {
        // Given
        InteractionDto.InteractionHistory history = new InteractionDto.InteractionHistory();
        history.setId(1L);
        history.setUserMessage("Hi");
        history.setAiResponse("Hello");

        given(interactionService.getInteractionHistory(anyLong(), anyString()))
                .willReturn(List.of(history));

        // When & Then
        mockMvc.perform(get("/api/magazines/1/interact")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userMessage").value("Hi"));
    }
}
