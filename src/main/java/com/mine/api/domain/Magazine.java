package com.mine.api.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "magazines")
public class Magazine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    // [NEW] 부제
    private String subtitle;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    // [NEW] 태그 목록 (JSON 형태로 저장)
    @Column(columnDefinition = "TEXT")
    private String tags;

    // [NEW] 무드보드 이미지 URL
    @Column(name = "moodboard_image_url")
    private String moodboardImageUrl;

    // [NEW] 무드보드 설명
    @Column(name = "moodboard_description", columnDefinition = "TEXT")
    private String moodboardDescription;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @com.fasterxml.jackson.annotation.JsonManagedReference
    @OneToMany(mappedBy = "magazine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MagazineSection> sections = new ArrayList<>();

    // ⭐ MagazineInteraction과의 관계 (CASCADE 추가)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "magazine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MagazineInteraction> interactions = new ArrayList<>();

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "magazine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MagazineLike> likes = new ArrayList<>();

    @Builder
    public Magazine(String title, String subtitle, String introduction, String coverImageUrl,
            String tags, String moodboardImageUrl, String moodboardDescription, User user) {
        this.title = title;
        this.subtitle = subtitle;
        this.introduction = introduction;
        this.coverImageUrl = coverImageUrl;
        this.tags = tags;
        this.moodboardImageUrl = moodboardImageUrl;
        this.moodboardDescription = moodboardDescription;
        this.user = user;
        this.createdAt = LocalDateTime.now();
        this.isPublic = false; // 기본값: 비공개
    }

    public void addSection(MagazineSection section) {
        sections.add(section);
        section.setMagazine(this);
    }

    // ⭐ Phase 1: 수정 메서드
    public void updateInfo(String title, String introduction) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title;
        }
        if (introduction != null && !introduction.trim().isEmpty()) {
            this.introduction = introduction;
        }
    }

    // ⭐ Phase 1: 소유자 확인 메서드
    public boolean isOwnedBy(User user) {
        return this.user.getId().equals(user.getId());
    }

    // ⭐ Phase 2: 공개/비공개 필드
    @Column(nullable = false)
    private Boolean isPublic = false;

    @Column(unique = true, length = 12)
    private String shareToken;

    // ⭐ Phase 2: 공개 설정 메서드
    public String setPublic(boolean isPublic) {
        this.isPublic = isPublic;

        if (isPublic && this.shareToken == null) {
            this.shareToken = generateShareToken();
        } else if (!isPublic) {
            this.shareToken = null;
        }

        return this.shareToken;
    }

    // ⭐ 낙관적 락 (동시 수정 방지)
    @jakarta.persistence.Version
    private Long version;

    /**
     * 보안 강화된 공유 토큰 생성
     * - SecureRandom 사용으로 예측 불가능성 향상
     * - Base64 URL-safe 인코딩
     * - 16자 길이로 충돌 확률 최소화
     */
    private String generateShareToken() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        byte[] bytes = new byte[9]; // 9 bytes * 8 = 72 bits -> Base64 12 chars
        random.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}
