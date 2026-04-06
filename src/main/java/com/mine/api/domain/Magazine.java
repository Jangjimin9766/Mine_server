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

    @Column(length = 500)
    private String title;

    @Column(name = "cover_image_url", length = 1000)
    private String coverImageUrl;

    // [NEW] 태그 목록 (JSON 형태로 저장)
    @Column(columnDefinition = "TEXT")
    private String tags;

    // [NEW] 무드보드 이미지 URL
    @Column(name = "moodboard_image_url", length = 1000)
    private String moodboardImageUrl;

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
    public Magazine(String title, String coverImageUrl,
            String tags, String moodboardImageUrl, User user) {
        this.title = title;
        this.coverImageUrl = coverImageUrl;
        this.tags = tags;
        this.moodboardImageUrl = moodboardImageUrl;
        this.user = user;
        this.createdAt = LocalDateTime.now();
    }

    public void addSection(MagazineSection section) {
        sections.add(section);
        section.setMagazine(this);
    }

    // ⭐ Phase 1: 수정 메서드
    public void updateInfo(String title) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title;
        }
    }

    // ⭐ 커버 이미지 변경
    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    // ⭐ 무드보드 이미지 변경
    public void setMoodboardImageUrl(String moodboardImageUrl) {
        this.moodboardImageUrl = moodboardImageUrl;
    }

    // ⭐ Phase 1: 소유자 확인 메서드
    public boolean isOwnedBy(User user) {
        return this.user.getId().equals(user.getId());
    }

    // ⭐ 낙관적 락 (동시 수정 방지)
    @jakarta.persistence.Version
    private Long version;
}
