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

    @Transactional
    public Long saveMagazine(MagazineCreateRequest request, String email) {
        com.mine.api.domain.User user = userRepository.findByEmail(email)
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

    public java.util.List<Magazine> getMagazinesByUser(String email) {
        com.mine.api.domain.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return magazineRepository.findAllByUser(user);
    }

    public Magazine getMagazineDetail(Long id, String email) {
        Magazine magazine = magazineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Magazine not found"));

        if (!magazine.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("Unauthorized access to this magazine");
        }

        return magazine;
    }
}
