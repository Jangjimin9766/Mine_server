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

@RestController
@RequestMapping("/api/moodboards")
@RequiredArgsConstructor
@Tag(name = "4. 무드보드 (Moodboard)", description = "AI 기반 무드보드 생성 API")
public class MoodboardController {

    private final MoodboardService moodboardService;

    @Operation(summary = "무드보드 생성", description = "사용자 취향을 분석하여 AI 배경화면을 생성합니다.")
    @PostMapping
    public ResponseEntity<MoodboardResponseDto> createMoodboard(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody MoodboardRequestDto requestDto) {

        String username = userDetails.getUsername();
        String s3Url = moodboardService.createMoodboard(username, requestDto);

        MoodboardResponseDto response = MoodboardResponseDto.builder()
                .image_url(s3Url)
                .description("Moodboard generated successfully")
                .build();

        return ResponseEntity.ok(response);
    }
}
