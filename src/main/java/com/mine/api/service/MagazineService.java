package com.mine.api.service;

import com.mine.api.domain.Magazine;
import com.mine.api.domain.MagazineSection;
import com.mine.api.dto.MagazineCreateRequest;
import com.mine.api.repository.MagazineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MagazineService {

    private final MagazineRepository magazineRepository;
    private final com.mine.api.repository.UserRepository userRepository;
    private final org.springframework.web.client.RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Value("${python.api.url}")
    private String pythonApiUrl;

    @Transactional
    public Long saveMagazine(MagazineCreateRequest request, String username) {
        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 1. Magazine 엔티티 생성
        Magazine magazine = Magazine.builder()
                .title(request.getTitle())
                .introduction(request.getIntroduction())
                .coverImageUrl(request.getCoverImageUrl())
                .user(user)
                .build();

        // 2. Section 엔티티 생성 및 연관관계 설정
        if (request.getSections() != null) {
            for (MagazineCreateRequest.SectionDto sectionDto : request.getSections()) {
                MagazineSection section = MagazineSection.builder()
                        .heading(sectionDto.getHeading())
                        .content(sectionDto.getContent())
                        .imageUrl(sectionDto.getImageUrl())
                        .layoutHint(sectionDto.getLayoutHint())
                        .build();
                magazine.addSection(section);
            }
        }

        // 3. 저장 (CascadeType.ALL로 인해 Section도 함께 저장됨)
        Magazine savedMagazine = magazineRepository.save(magazine);
        return savedMagazine.getId();
    }

    public java.util.List<Magazine> getMagazinesByUser(String username) {
        com.mine.api.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return magazineRepository.findAllByUser(user);
    }

    public Magazine getMagazineDetail(Long id, String username) {
        Magazine magazine = magazineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Magazine not found"));

        if (!magazine.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Unauthorized access to this magazine");
        }

        return magazine;
    }

    @Transactional
    public Long generateAndSaveMagazine(com.mine.api.dto.MagazineGenerationRequest request, String username) {
        // 1. Python 서버로 요청
        // Python 서버가 기대하는 요청 바디: {"topic": "...", "user_mood": "...", "user_email":
        // "..."}
        // 주의: Python 서버는 여전히 "user_email"이라는 키를 쓰고 있을 수 있습니다.
        // 하지만 우리는 이제 식별자로 username을 씁니다. Python이 로깅용으로만 쓴다면 username을 보내도 무방합니다.
        // 만약 Python이 진짜 이메일을 원한다면 DB에서 조회해서 보내야 합니다.
        // 여기서는 일단 username을 보냅니다. (Python 서버 수정 없이 "user_email" 키에 username 값을 넣음)
        java.util.Map<String, String> pythonRequest = new java.util.HashMap<>();
        pythonRequest.put(com.mine.api.common.AppConstants.KEY_TOPIC, request.getTopic());
        pythonRequest.put(com.mine.api.common.AppConstants.KEY_USER_MOOD, request.getUserMood());
        pythonRequest.put(com.mine.api.common.AppConstants.KEY_USER_EMAIL, username);

        MagazineCreateRequest generatedData = restTemplate.postForObject(pythonApiUrl, pythonRequest,
                MagazineCreateRequest.class);

        if (generatedData == null) {
            throw new RuntimeException("Failed to generate magazine from AI server");
        }

        // 2. 받은 데이터로 저장 로직 수행
        return saveMagazine(generatedData, username);
    }
}
