package com.mine.api.controller;

import com.mine.api.dto.InterestDto;
import com.mine.api.service.InterestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
@AutoConfigureMockMvc(addFilters = false) // Security 필터 비활성화
class InterestControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private InterestService interestService;

        @Test
        @DisplayName("전체 관심사 목록 조회 성공")
        @WithMockUser // 비로그인 대신 MockUser 사용으로 변경하여 테스트 안정성 확보
        void getAllInterests_Success() throws Exception {
                // Given
                List<InterestDto.InterestResponse> interests = List.of(
                                InterestDto.InterestResponse.builder()
                                                .id(1L).code("TRAVEL").name("여행").category("활동").build(),
                                InterestDto.InterestResponse.builder()
                                                .id(2L).code("FOOD").name("푸드").category("라이프스타일").build());
                given(interestService.getAllInterests()).willReturn(interests);

                // When & Then
                mockMvc.perform(get("/api/interests"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].code").value("TRAVEL"))
                                .andExpect(jsonPath("$[0].name").value("여행"));
        }

        @Test
        @DisplayName("내 관심사 조회 성공")
        @WithMockUser(username = "testuser")
        void getMyInterests_Success() throws Exception {
                // Given
                List<InterestDto.InterestResponse> myInterests = List.of(
                                InterestDto.InterestResponse.builder()
                                                .id(1L).code("TRAVEL").name("여행").category("활동").build());
                given(interestService.getUserInterests("testuser")).willReturn(myInterests);

                // When & Then
                mockMvc.perform(get("/api/interests/me"))
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
