package com.mine.api.service;

import com.mine.api.domain.User;
import com.mine.api.dto.UserDto;
import com.mine.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Test
    void 프로필_업데이트_테스트() {
        // given
        String username = "testuser";
        // UpdateRequest 생성자 인자: nickname, bio, email, profileImageUrl
        UserDto.UpdateRequest request = new UserDto.UpdateRequest("newNick", "newBio", "test@example.com", "newImg");
        
        User user = User.builder()
                .username(username)
                .email("test@example.com")
                .nickname("oldNick")
                .build();

        given(userRepository.findByUsername(username)).willReturn(Optional.of(user));

        // when & then
        assertDoesNotThrow(() -> {
            userService.updateProfile(username, request);
        });
    }
}
