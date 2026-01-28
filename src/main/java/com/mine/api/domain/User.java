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
@Table(name = "users")
@org.hibernate.annotations.SQLRestriction("deleted = false")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username; // 아이디

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    private Role role;

    private LocalDateTime createdAt;

    private String profileImageUrl;
    private LocalDateTime updatedAt;

    private Boolean deleted = false;
    private LocalDateTime deletedAt;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true; // 기본: 공개 계정

    // ⭐ Optimization: N+1 문제 해결을 위한 가상 컬럼 (Subquery)
    @org.hibernate.annotations.Formula("(SELECT count(*) FROM follows f WHERE f.following_id = id)")
    private int followerCount;

    @org.hibernate.annotations.Formula("(SELECT count(*) FROM follows f WHERE f.follower_id = id)")
    private int followingCount;

    @org.hibernate.annotations.Formula("(SELECT count(*) FROM magazines m WHERE m.user_id = id)")
    private int magazineCount;

    @Builder
    public User(String username, String email, String password, String nickname, Role role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }

    // ⭐ Phase 7: 비밀번호 변경
    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    // ⭐ Phase 5: 프로필 수정
    public void updateProfile(String nickname, String profileImageUrl) {
        if (nickname != null)
            this.nickname = nickname;
        if (profileImageUrl != null)
            this.profileImageUrl = profileImageUrl;
        this.updatedAt = LocalDateTime.now();
    }

    // ⭐ Phase 6: 회원 탈퇴 (Soft Delete)
    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    // ⭐ 계정 공개/비공개 설정
    public void setPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Boolean getIsPublic() {
        return this.isPublic != null ? this.isPublic : true; // NULL 처리
    }
}
