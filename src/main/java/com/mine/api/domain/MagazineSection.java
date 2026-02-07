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

    private String heading;

    // ===== 새로 추가된 필드 =====

    /**
     * 섹션 썸네일 (커버 이미지)
     * 섹션 목록에서 미리보기로 사용
     */
    @Column(name = "thumbnail_url", length = 500)
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

    // ===== Deprecated 필드 (하위 호환용) =====

    /**
     * @deprecated paragraphs[].text 사용 권장
     */
    @Deprecated
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * @deprecated thumbnailUrl 또는 paragraphs[].imageUrl 사용 권장
     */
    @Deprecated
    @Column(name = "image_url")
    private String imageUrl;

    /**
     * @deprecated 삭제 예정
     */
    @Deprecated
    private String caption;

    // ===== 기존 필드 =====

    @Column(name = "layout_hint")
    private String layoutHint;

    // 그리드 표시 순서
    @Column(name = "display_order")
    private Integer displayOrder;

    // 레이아웃 타입: 'hero', 'quote', 'split_left', 'split_right', 'basic' 등
    @Column(name = "layout_type")
    private String layoutType;

    @com.fasterxml.jackson.annotation.JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magazine_id")
    private Magazine magazine;

    @Builder
    public MagazineSection(String heading, String content, String imageUrl, String layoutHint,
            String layoutType, String caption, Integer displayOrder, String thumbnailUrl) {
        this.heading = heading;
        this.content = content;
        this.imageUrl = imageUrl;
        this.layoutHint = layoutHint;
        this.layoutType = layoutType;
        this.caption = caption;
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

    public void updateContent(String heading, String content, String imageUrl) {
        this.heading = heading;
        this.content = content;
        this.imageUrl = imageUrl;
    }

    public void update(String heading, String content, String imageUrl, String layoutHint,
            String layoutType, String caption) {
        this.heading = heading;
        this.content = content;
        this.imageUrl = imageUrl;
        this.layoutHint = layoutHint;
        this.layoutType = layoutType;
        this.caption = caption;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
}
