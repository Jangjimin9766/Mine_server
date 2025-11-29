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
@Table(name = "magazine_interactions")
public class MagazineInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magazine_id", nullable = false)
    private Magazine magazine;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String userMessage;

    @Column(columnDefinition = "TEXT")
    private String aiResponse;

    @Column(nullable = false)
    private String actionType; // "regenerate_section", "add_section", "change_tone", etc.

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public MagazineInteraction(Magazine magazine, String userMessage, String aiResponse, String actionType) {
        this.magazine = magazine;
        this.userMessage = userMessage;
        this.aiResponse = aiResponse;
        this.actionType = actionType;
        this.createdAt = LocalDateTime.now();
    }
}
