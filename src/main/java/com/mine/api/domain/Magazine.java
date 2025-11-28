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

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @com.fasterxml.jackson.annotation.JsonManagedReference
    @OneToMany(mappedBy = "magazine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MagazineSection> sections = new ArrayList<>();

    @Builder
    public Magazine(String title, String introduction, String coverImageUrl, User user) {
        this.title = title;
        this.introduction = introduction;
        this.coverImageUrl = coverImageUrl;
        this.user = user;
        this.createdAt = LocalDateTime.now();
    }

    public void addSection(MagazineSection section) {
        this.sections.add(section);
        section.setMagazine(this);
    }
}
