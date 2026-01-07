package com.mine.api.controller;

import com.mine.api.dto.MoodboardRequestDto;
import com.mine.api.dto.MoodboardResponseDto;
import com.mine.api.service.MoodboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "4. 무드보드 (Moodboard) 🎨", description = "무드보드(AI 배경화면) 생성 및 관리 API")
@RestController
@RequestMapping("/api/moodboards")
@RequiredArgsConstructor
public class MoodboardController {

    private final MoodboardService moodboardService;

    @Operation(summary = "무드보드 생성", description = "사용자 취향을 분석하여 AI 배경화면을 생성합니다.")
    @PostMapping
    public ResponseEntity<MoodboardResponseDto> createMoodboard(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody MoodboardRequestDto requestDto) {

        // UserDetails에서 userId를 가져오는 로직이 필요하지만, 현재 UserDetails는 Spring Security User일 수
        // 있음.
        // 여기서는 username으로 User를 조회하거나, CustomUserDetails를 사용한다고 가정.
        // MVP 구현을 위해 Service에서 username을 받아 처리하도록 수정하거나,
        // 현재 구조상 UserDetails.getUsername()을 넘기는 것이 안전함.
        // 하지만 Service는 userId를 받고 있음.
        // -> Service를 수정하여 username을 받도록 변경하거나, Controller에서 User를 조회해야 함.
        // 효율성을 위해 Service에서 username으로 조회하도록 변경하는 것이 좋음.
        // 일단 여기서는 Service의 시그니처를 변경하지 않고, 추후 리팩토링 고려.
        // 임시로 UserDetails가 CustomUserDetails라고 가정하고 ID를 꺼내거나,
        // Service에 username을 넘기는 방식으로 수정 필요.
        // Service 코드를 방금 작성했으므로, Service를 수정하는 것이 빠름.

        // *REVISION*: Service를 username을 받도록 수정하겠습니다.
        // 하지만 지금은 Controller 작성이므로, 일단 컴파일 되도록 작성하고 Service를 수정합니다.

        String username = userDetails.getUsername();
        String s3Url = moodboardService.createMoodboard(username, requestDto);

        MoodboardResponseDto response = MoodboardResponseDto.builder()
                .image_url(s3Url)
                .description("Moodboard generated successfully")
                .build();

        return ResponseEntity.ok(response);
    }
}
