package com.mine.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mine.api.dto.UserDto;
import com.mine.api.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private UserService userService;

        @Autowired
        private ObjectMapper objectMapper;

        // --- Follow System Tests ---

        @Test
        @WithMockUser(username = "follower")
        void followUser() throws Exception {
                UserDto.FollowResponse response = new UserDto.FollowResponse(true, 10);
                given(userService.followUser(eq(1L), eq("follower"))).willReturn(response);

                mockMvc.perform(post("/api/users/1/follow")
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.followed").value(true))
                                .andExpect(jsonPath("$.followerCount").value(10));
        }

        @Test
        @WithMockUser(username = "follower")
        void unfollowUser() throws Exception {
                UserDto.FollowResponse response = new UserDto.FollowResponse(false, 9);
                given(userService.unfollowUser(eq(1L), eq("follower"))).willReturn(response);

                mockMvc.perform(delete("/api/users/1/follow")
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.followed").value(false))
                                .andExpect(jsonPath("$.followerCount").value(9));
        }

        @Test
        @WithMockUser(username = "user")
        void getFollowers() throws Exception {
                UserDto.ProfileResponse profile = UserDto.ProfileResponse.builder()
                                .id(2L)
                                .username("follower")
                                .nickname("Follower")
                                .build();
                Page<UserDto.ProfileResponse> page = new PageImpl<>(List.of(profile), PageRequest.of(0, 10), 1);

                given(userService.getFollowers(eq(1L), eq("user"), any(Pageable.class))).willReturn(page);

                mockMvc.perform(get("/api/users/1/followers"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].username").value("follower"));
        }

        @Test
        @WithMockUser(username = "user")
        void getFollowing() throws Exception {
                UserDto.ProfileResponse profile = UserDto.ProfileResponse.builder()
                                .id(3L)
                                .username("following")
                                .nickname("Following")
                                .build();
                Page<UserDto.ProfileResponse> page = new PageImpl<>(List.of(profile), PageRequest.of(0, 10), 1);

                given(userService.getFollowing(eq(1L), eq("user"), any(Pageable.class))).willReturn(page);

                mockMvc.perform(get("/api/users/1/following"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].username").value("following"));
        }

        // --- Profile Management Tests ---

        @Test
        @WithMockUser(username = "user")
        void getMyProfile() throws Exception {
                UserDto.ProfileResponse profile = UserDto.ProfileResponse.builder()
                                .id(1L)
                                .username("user")
                                .nickname("User")
                                .build();

                given(userService.getMyProfile("user")).willReturn(profile);

                mockMvc.perform(get("/api/users/me"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("user"));
        }

        @Test
        @WithMockUser(username = "user")
        void updateMyProfile() throws Exception {
                UserDto.UpdateRequest request = new UserDto.UpdateRequest("NewNick", "NewBio", "new@example.com",
                                "img.jpg");
                UserDto.ProfileResponse response = UserDto.ProfileResponse.builder()
                                .id(1L)
                                .username("user")
                                .nickname("NewNick")
                                .bio("NewBio")
                                .email("new@example.com")
                                .profileImageUrl("img.jpg")
                                .build();

                given(userService.updateProfile(eq("user"), any(UserDto.UpdateRequest.class))).willReturn(response);

                mockMvc.perform(patch("/api/users/me")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.nickname").value("NewNick"))
                                .andExpect(jsonPath("$.bio").value("NewBio"));
        }

        @Test
        @WithMockUser(username = "viewer")
        void getUserProfile() throws Exception {
                UserDto.ProfileResponse profile = UserDto.ProfileResponse.builder()
                                .id(1L)
                                .username("target")
                                .nickname("Target")
                                .build();

                given(userService.getUserProfile(1L, "viewer")).willReturn(profile);

                mockMvc.perform(get("/api/users/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("target"));
        }

        // --- Withdrawal Tests ---

        @Test
        @WithMockUser(username = "user")
        void withdrawUser() throws Exception {
                doNothing().when(userService).withdrawUser("user");
                doNothing().when(userService).logout(anyString());

                mockMvc.perform(delete("/api/users/me")
                                .header("Authorization", "Bearer test-token")
                                .with(csrf()))
                                .andExpect(status().isNoContent());
        }
}
