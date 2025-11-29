package com.mine.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mine.api.domain.Magazine;
import com.mine.api.domain.User;
import com.mine.api.dto.MagazineDto;
import com.mine.api.dto.MagazineGenerationRequest;
import com.mine.api.service.MagazineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MagazineController.class)
@AutoConfigureMockMvc(addFilters = false) // Security Filter 비활성화 (단위 테스트 집중)
class MagazineControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private MagazineService magazineService;

        @Autowired
        private ObjectMapper objectMapper;

        private Magazine magazine;

        @BeforeEach
        void setUp() {
                User user = User.builder()
                                .email("test@example.com")
                                .username("testuser")
                                .role(com.mine.api.domain.Role.USER)
                                .build();

                magazine = org.mockito.Mockito.mock(Magazine.class);
                given(magazine.getTitle()).willReturn("Test Magazine");
                given(magazine.getIntroduction()).willReturn("Test Intro");
                given(magazine.getUser()).willReturn(user);
        }

        @Test
        @WithMockUser(username = "testuser")
        void getMyMagazines() throws Exception {
                Page<Magazine> page = new PageImpl<>(new java.util.ArrayList<>(java.util.Arrays.asList(magazine)),
                                org.springframework.data.domain.PageRequest.of(0, 10), 1);
                given(magazineService.getMyMagazinesPage(eq("testuser"), any(Pageable.class))).willReturn(page);

                mockMvc.perform(get("/api/magazines"))
                                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].title").value("Test Magazine"));
        }

        @Test
        @WithMockUser(username = "testuser")
        void getMagazineDetail() throws Exception {
                given(magazineService.getMagazineDetail(1L, "testuser")).willReturn(magazine);

                mockMvc.perform(get("/api/magazines/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Test Magazine"));
        }

        @Test
        @WithMockUser(username = "testuser")
        void createMagazine() throws Exception {
                MagazineGenerationRequest request = new MagazineGenerationRequest();
                request.setTopic("Travel");
                request.setUserMood("Happy");

                given(magazineService.generateAndSaveMagazine(any(MagazineGenerationRequest.class), eq("testuser")))
                                .willReturn(1L);

                mockMvc.perform(post("/api/magazines")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").value(1L));
        }

        @Test
        @WithMockUser(username = "testuser")
        void deleteMagazine() throws Exception {
                doNothing().when(magazineService).deleteMagazine(1L, "testuser");

                mockMvc.perform(delete("/api/magazines/1")
                                .with(csrf()))
                                .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(username = "testuser")
        void updateMagazine() throws Exception {
                MagazineDto.UpdateRequest request = new MagazineDto.UpdateRequest();
                request.setTitle("Updated Title");

                doNothing().when(magazineService).updateMagazine(eq(1L), any(MagazineDto.UpdateRequest.class),
                                eq("testuser"));

                mockMvc.perform(patch("/api/magazines/1")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("수정되었습니다"));
        }
}
