package com.mine.api.repository;

import com.mine.api.domain.Magazine;
import com.mine.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MagazineRepository extends JpaRepository<Magazine, Long> {
        List<Magazine> findAllByUser(User user);

        // ⭐ N+1 쿼리 방지: sections와 user를 한 번에 조회
        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT m FROM Magazine m " +
                        "LEFT JOIN FETCH m.sections " +
                        "LEFT JOIN FETCH m.user " +
                        "WHERE m.user.username = :username " +
                        "ORDER BY m.createdAt DESC")
        java.util.List<Magazine> findByUserUsernameWithSections(
                        @org.springframework.data.repository.query.Param("username") String username);

        // ⭐ Phase 2: 공유 토큰으로 조회
        java.util.Optional<Magazine> findByShareToken(String shareToken);

        // ⭐ Phase 2: 키워드 검색 (제목 + 소개 + 태그 + 섹션 제목/본문)
        // 본인 매거진은 비공개여도 검색됨
        @org.springframework.data.jpa.repository.Query(value = "SELECT DISTINCT m FROM Magazine m " +
                        "LEFT JOIN m.sections s " +
                        "WHERE (m.title LIKE CONCAT('%', :keyword, '%') " +
                        "OR m.introduction LIKE CONCAT('%', :keyword, '%') " +
                        "OR m.tags LIKE CONCAT('%', :keyword, '%') " +
                        "OR s.heading LIKE CONCAT('%', :keyword, '%') " +
                        "OR s.content LIKE CONCAT('%', :keyword, '%')) " +
                        "AND (m.isPublic = true OR m.user.username = :username)", countQuery = "SELECT COUNT(DISTINCT m) FROM Magazine m "
                                        +
                                        "LEFT JOIN m.sections s " +
                                        "WHERE (m.title LIKE CONCAT('%', :keyword, '%') " +
                                        "OR m.introduction LIKE CONCAT('%', :keyword, '%') " +
                                        "OR m.tags LIKE CONCAT('%', :keyword, '%') " +
                                        "OR s.heading LIKE CONCAT('%', :keyword, '%') " +
                                        "OR s.content LIKE CONCAT('%', :keyword, '%')) " +
                                        "AND (m.isPublic = true OR m.user.username = :username)")
        org.springframework.data.domain.Page<Magazine> searchByKeyword(
                        @org.springframework.data.repository.query.Param("keyword") String keyword,
                        @org.springframework.data.repository.query.Param("username") String username,
                        org.springframework.data.domain.Pageable pageable);

        // ⭐ Phase 2: 태그 검색 (Magazine에 tags 필드 추가 후 사용)
        // @org.springframework.data.jpa.repository.Query("SELECT DISTINCT m FROM
        // Magazine m JOIN m.tags t " +
        // "WHERE t IN :tags AND m.isPublic = true")
        // org.springframework.data.domain.Page<Magazine> findByTagsIn(
        // @org.springframework.data.repository.query.Param("tags")
        // java.util.List<String> tags,
        // org.springframework.data.domain.Pageable pageable);

        // ⭐ Phase 2: 내 매거진 - FETCH JOIN으로 N+1 및 LazyInitializationException 방지
        @org.springframework.data.jpa.repository.Query(value = "SELECT DISTINCT m FROM Magazine m " +
                        "LEFT JOIN FETCH m.user " +
                        "LEFT JOIN FETCH m.sections " +
                        "WHERE m.user.username = :username", countQuery = "SELECT COUNT(m) FROM Magazine m WHERE m.user.username = :username")
        org.springframework.data.domain.Page<Magazine> findByUserUsername(
                        @org.springframework.data.repository.query.Param("username") String username,
                        org.springframework.data.domain.Pageable pageable);

        long countByUser(User user);

        // ⭐ Phase 4: 개인화 피드 (팔로잉 + 관심사 키워드)
        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT m FROM Magazine m " +
                        "LEFT JOIN FETCH m.user " +
                        "LEFT JOIN FETCH m.sections " +
                        "WHERE (m.user IN :followings " +
                        "OR (m.title LIKE %:keyword% OR m.introduction LIKE %:keyword%)) " +
                        "AND m.isPublic = true " +
                        "ORDER BY m.createdAt DESC")
        org.springframework.data.domain.Page<Magazine> findPersonalizedFeed(
                        @org.springframework.data.repository.query.Param("followings") List<User> followings,
                        @org.springframework.data.repository.query.Param("keyword") String keyword,
                        org.springframework.data.domain.Pageable pageable);
}
