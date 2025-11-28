package com.mine.api.controller;

import com.mine.api.dto.MagazineCreateRequest;
import com.mine.api.service.MagazineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalApiController {

    private final MagazineService magazineService;

    @PostMapping("/magazine")
    public ResponseEntity<Long> createMagazine(@RequestBody MagazineCreateRequest request) {
        Long magazineId = magazineService.saveMagazine(request, request.getUserEmail());
        return ResponseEntity.ok(magazineId);
    }
}
