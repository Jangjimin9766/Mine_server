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

    @com.fasterxml.jackson.annotation.JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magazine_id")
    private Magazine magazine;

    @Builder
    public MagazineSection(String heading, String content, String imageUrl, String layoutHint) {
        this.heading = heading;
        this.content = content;
        this.imageUrl = imageUrl;
        this.layoutHint = layoutHint;
    }

    public void setMagazine(Magazine magazine) {
        this.magazine = magazine;
    }
}
