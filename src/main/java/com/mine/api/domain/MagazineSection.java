package com.mine.api.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "magazine_sections")
public class MagazineSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String heading;

    // ===== 새로 추가된 필드 =====

    /**
     * 섹션 썸네일 (커버 이미지)
     * 섹션 목록에서 미리보기로 사용
     */
    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    /**
     * 문단 배열 (기본 3개)
     * 각 문단은 subtitle + text + imageUrl 세트로 구성
     * React에서 지그재그 레이아웃으로 렌더링
     */
    @com.fasterxml.jackson.annotation.JsonManagedReference
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<Paragraph> paragraphs = new ArrayList<>();

    // ⭐ SectionViewHistory cascade: 섹션 삭제 시 열람 기록 자동 삭제 (FK 위반 방지)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SectionViewHistory> viewHistories = new ArrayList<>();

    // ===== 기존 필드 =====

    // 그리드 표시 순서
    @Column(name = "display_order")
    private Integer displayOrder;

    @com.fasterxml.jackson.annotation.JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magazine_id")
    private Magazine magazine;

    @Builder
    public MagazineSection(String heading, Integer displayOrder, String thumbnailUrl) {
        this.heading = heading;
        this.displayOrder = displayOrder;
        this.thumbnailUrl = thumbnailUrl;
    }

    public void setMagazine(Magazine magazine) {
        this.magazine = magazine;
    }

    /**
     * 문단 추가 헬퍼 메서드
     */
    public void addParagraph(Paragraph paragraph) {
        this.paragraphs.add(paragraph);
        paragraph.setSection(this);
    }

    public void removeParagraph(Paragraph paragraph) {
        this.paragraphs.remove(paragraph);
        paragraph.setSection(null);
    }

    public void update(String heading) {
        this.heading = heading;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
}
