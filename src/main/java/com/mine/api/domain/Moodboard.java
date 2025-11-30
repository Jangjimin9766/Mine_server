package com.mine.api.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "moodboards")
public class Moodboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "image_url", length = 2048, nullable = false)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Moodboard(Long userId, String imageUrl, String prompt) {
        this.userId = userId;
        this.imageUrl = imageUrl;
        this.prompt = prompt;
        this.createdAt = LocalDateTime.now();
    }
}
