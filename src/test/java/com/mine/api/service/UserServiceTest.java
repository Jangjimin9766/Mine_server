package com.mine.api.service;

import com.mine.api.dto.UserDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class UserServiceTest {
    // UserService의 특정 메서드만 테스트
    @Test
    void 닉네임이_2자_미만이면_예외발생() {
        // given (준비)
        UserDto.UpdateRequest request = new UserDto.UpdateRequest("짧", null, null);

        // when & then (실행 및 검증)
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateProfile("testuser", request);
        });
    }
}
