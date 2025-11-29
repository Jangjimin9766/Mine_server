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
@Table(name = "magazine_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "magazine_id" })
})
public class MagazineLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magazine_id", nullable = false)
    private Magazine magazine;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public MagazineLike(User user, Magazine magazine) {
        this.user = user;
        this.magazine = magazine;
        this.createdAt = LocalDateTime.now();
    }
}
