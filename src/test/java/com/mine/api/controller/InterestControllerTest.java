package com.mine.api.controller;

import com.mine.api.dto.InterestDto;
import com.mine.api.service.InterestService;
import com.mine.api.domain.Interest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InterestController.class)
class InterestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InterestService interestService;

    @Test
    @DisplayName("전체 관심사 목록 조회 성공")
    @WithMockUser
    void getAllInterests_Success() throws Exception {
        // Given
        List<InterestDto.InterestResponse> interests = List.of(
                new InterestDto.InterestResponse(Interest.TRAVEL),
                new InterestDto.InterestResponse(Interest.FOOD));
        given(interestService.getAllInterests()).willReturn(interests);

        // When & Then
        mockMvc.perform(get("/api/interests")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("TRAVEL"))
                .andExpect(jsonPath("$[0].displayName").value("여행"));
    }

    @Test
    @DisplayName("내 관심사 조회 성공")
    @WithMockUser(username = "testuser")
    void getMyInterests_Success() throws Exception {
        // Given
        List<InterestDto.InterestResponse> myInterests = List.of(
                new InterestDto.InterestResponse(Interest.TRAVEL));
        given(interestService.getUserInterests("testuser")).willReturn(myInterests);

        // When & Then
        mockMvc.perform(get("/api/interests/me")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("TRAVEL"));
    }

    @Test
    @DisplayName("내 관심사 저장 성공")
    @WithMockUser(username = "testuser")
    void updateMyInterests_Success() throws Exception {
        // Given
        String requestBody = "{\"interests\": [\"TRAVEL\", \"FOOD\"]}";

        // When & Then
        mockMvc.perform(put("/api/interests/me")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        verify(interestService).updateUserInterests(anyString(), anyList());
    }
}
