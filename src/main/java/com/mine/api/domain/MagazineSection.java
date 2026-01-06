package com.mine.api.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "magazine_sections")
public class MagazineSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String heading;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "layout_hint")
    private String layoutHint;

    // 그리드 표시 순서
    @Column(name = "display_order")
    private Integer displayOrder;

    // [NEW] 레이아웃 타입: 'hero', 'quote', 'split_left', 'split_right', 'basic' 등
    @Column(name = "layout_type")
    private String layoutType;

    // [NEW] 이미지 캡션
    private String caption;

    @com.fasterxml.jackson.annotation.JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magazine_id")
    private Magazine magazine;

    @Builder
    public MagazineSection(String heading, String content, String imageUrl, String layoutHint,
            String layoutType, String caption) {
        this.heading = heading;
        this.content = content;
        this.imageUrl = imageUrl;
        this.layoutHint = layoutHint;
        this.layoutType = layoutType;
        this.caption = caption;
    }

    public void setMagazine(Magazine magazine) {
        this.magazine = magazine;
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
}
