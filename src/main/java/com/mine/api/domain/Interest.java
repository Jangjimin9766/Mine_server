package com.mine.api.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관심분야 엔티티 (DB 테이블)
 * 기존 Enum에서 마이그레이션됨
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "interests")
public class Interest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String code; // "FASHION", "BEAUTY" 등

    @Column(nullable = false, length = 50)
    private String name; // "패션", "뷰티" 등

    @Column(length = 50)
    private String category; // "라이프스타일", "문화/예술" 등

    @Builder
    public Interest(String code, String name, String category) {
        this.code = code;
        this.name = name;
        this.category = category;
    }
}
