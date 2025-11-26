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

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "magazine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MagazineSection> sections = new ArrayList<>();

    @Builder
    public Magazine(String title, String introduction, String coverImageUrl, Long userId) {
        this.title = title;
        this.introduction = introduction;
        this.coverImageUrl = coverImageUrl;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
    }

    public void addSection(MagazineSection section) {
        this.sections.add(section);
        section.setMagazine(this);
    }
}
