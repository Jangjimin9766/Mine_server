package com.mine.api.controller;

import com.mine.api.dto.InterestDto;
import com.mine.api.service.InterestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관심사 (Interest)", description = "사용자 관심사 선택 및 조회 API")
@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
public class InterestController {

    private final InterestService interestService;

    @Operation(summary = "전체 관심사 목록 조회", description = "선택 가능한 모든 관심사 카테고리를 조회합니다.")
    @GetMapping
    public ResponseEntity<List<InterestDto.InterestResponse>> getAllInterests() {
        return ResponseEntity.ok(interestService.getAllInterests());
    }

    @Operation(summary = "내 관심사 조회", description = "로그인한 사용자가 선택한 관심사 목록을 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<List<InterestDto.InterestResponse>> getMyInterests(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(interestService.getUserInterests(userDetails.getUsername()));
    }

    @Operation(summary = "내 관심사 저장", description = "관심사를 선택합니다. 최대 3개까지 선택 가능합니다.")
    @PutMapping("/me")
    public ResponseEntity<Void> updateMyInterests(
            @RequestBody InterestDto.UpdateInterestsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        interestService.updateUserInterests(userDetails.getUsername(), request.getInterests());
        return ResponseEntity.ok().build();
    }
}
