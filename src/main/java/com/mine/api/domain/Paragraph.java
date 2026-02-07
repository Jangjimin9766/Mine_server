package com.mine.api.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 섹션 내 문단 엔티티
 * 각 문단은 subtitle, text, imageUrl 세트로 구성
 * React에서 지그재그 레이아웃 렌더링에 사용
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "paragraph")
public class Paragraph {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private MagazineSection section;

    /**
     * 문단 소제목 (예: "국밥의 성지, 서면")
     */
    @Column(nullable = false, length = 200)
    private String subtitle;

    /**
     * 문단 본문 (150-300자)
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    /**
     * 문단 이미지 URL
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * 표시 순서
     */
    @Column(name = "display_order")
    private Integer displayOrder;

    @Builder
    public Paragraph(String subtitle, String text, String imageUrl, Integer displayOrder) {
        this.subtitle = subtitle;
        this.text = text;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }

    public void setSection(MagazineSection section) {
        this.section = section;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
