package com.mine.api.controller;

import com.mine.api.service.DummyDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DummyDataController {

    private final DummyDataService dummyDataService;

    // 개발망(혹은 초기 세팅용)에서만 사용하는 일회성 트리거 엔드포인트
    @GetMapping("/init-dummy-magazines")
    public ResponseEntity<String> initDummyData() {
        dummyDataService.generateDummyData();
        return ResponseEntity.ok("Dummy data generation (5 users, 15 magazines) triggered. Check logs for progress.");
    }
}
