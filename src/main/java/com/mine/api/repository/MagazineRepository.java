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

        // ⭐ 태그로 매거진 검색 (커서 기반 페이지네이션)
        @org.springframework.data.jpa.repository.Query("SELECT m FROM Magazine m WHERE m.tags LIKE %:tag% AND (:lastId IS NULL OR m.id < :lastId) ORDER BY m.id DESC")
        org.springframework.data.domain.Page<Magazine> findByTag(
                        @org.springframework.data.repository.query.Param("tag") String tag,
                        @org.springframework.data.repository.query.Param("lastId") Long lastId,
                        org.springframework.data.domain.Pageable pageable);

        // ⭐ 특정 유저의 매거진 목록 조회 (페이징)
        org.springframework.data.domain.Page<Magazine> findAllByUserId(Long userId,
                        org.springframework.data.domain.Pageable pageable);

        // ⭐ 공개된 매거진 전체 조회
        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "user", "sections" })
        @org.springframework.data.jpa.repository.Query("SELECT m FROM Magazine m WHERE m.user.isPublic = true")
        org.springframework.data.domain.Page<Magazine> findByUserIsPublicTrue(
                        org.springframework.data.domain.Pageable pageable);

        // ⭐ 키워드 검색 (제목 + 소개 + 태그 + 섹션 제목/본문)
        // 본인 매거진 또는 공개 계정의 매거진만 검색됨
        // [PERFORMANCE] Full-Text Index 사용을 위해 Native Query로 변경
        @org.springframework.data.jpa.repository.Query(value = "SELECT DISTINCT m.* FROM magazines m " +
                        "LEFT JOIN magazine_sections s ON m.id = s.magazine_id " +
                        "LEFT JOIN users u ON m.user_id = u.id " +
                        "WHERE (MATCH(m.title, m.introduction, m.tags) AGAINST(:keyword IN BOOLEAN MODE) " +
                        "OR s.heading LIKE CONCAT('%', :keyword, '%') " + // 섹션은 아직 Index 미적용 시 LIKE 유지 (또는 추가 적용)
                        "OR s.content LIKE CONCAT('%', :keyword, '%')) " +
                        "AND (m.is_public = true OR u.username = :username)", countQuery = "SELECT COUNT(DISTINCT m.id) FROM magazines m "
                                        +
                                        "LEFT JOIN magazine_sections s ON m.id = s.magazine_id " +
                                        "LEFT JOIN users u ON m.user_id = u.id " +
                                        "WHERE (MATCH(m.title, m.introduction, m.tags) AGAINST(:keyword IN BOOLEAN MODE) "
                                        +
                                        "OR s.heading LIKE CONCAT('%', :keyword, '%') " +
                                        "OR s.content LIKE CONCAT('%', :keyword, '%')) " +
                                        "AND (m.is_public = true OR u.username = :username)", nativeQuery = true)
        org.springframework.data.domain.Page<Magazine> searchByKeyword(
                        @org.springframework.data.repository.query.Param("keyword") String keyword,
                        @org.springframework.data.repository.query.Param("username") String username,
                        org.springframework.data.domain.Pageable pageable);

        // ⭐ Phase 2: 내 매거진 - FETCH JOIN으로 N+1 및 LazyInitializationException 방지
        @org.springframework.data.jpa.repository.Query(value = "SELECT DISTINCT m FROM Magazine m " +
                        "LEFT JOIN FETCH m.user " +
                        "LEFT JOIN FETCH m.sections " +
                        "WHERE m.user.username = :username", countQuery = "SELECT COUNT(m) FROM Magazine m WHERE m.user.username = :username")
        org.springframework.data.domain.Page<Magazine> findByUserUsername(
                        @org.springframework.data.repository.query.Param("username") String username,
                        org.springframework.data.domain.Pageable pageable);

        long countByUser(User user);

        // ⭐ 개인화 피드 (커서 기반) - 팔로잉 + 관심사
        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT m FROM Magazine m " +
                        "LEFT JOIN FETCH m.user " +
                        "LEFT JOIN FETCH m.sections " +
                        "WHERE (m.user IN :followings " +
                        "OR m.tags LIKE %:keyword% OR m.title LIKE %:keyword%) " +
                        "AND m.user.isPublic = true " +
                        "AND (:lastId IS NULL OR m.id < :lastId) " +
                        "ORDER BY m.id DESC")
        java.util.List<Magazine> findPersonalizedFeedCursor(
                        @org.springframework.data.repository.query.Param("followings") java.util.List<User> followings,
                        @org.springframework.data.repository.query.Param("keyword") String keyword,
                        @org.springframework.data.repository.query.Param("lastId") Long lastId,
                        org.springframework.data.domain.Pageable pageable);

        // ⭐ 좋아요 + 관심사 기반 피드 (커서 기반) - 사용자 공개 여부만 체크
        // keywords: 사용자 관심사 + 좋아요한 매거진의 태그
        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT m FROM Magazine m " +
                        "LEFT JOIN FETCH m.user " +
                        "LEFT JOIN FETCH m.sections " +
                        "WHERE (m.tags LIKE %:keyword1% OR m.tags LIKE %:keyword2% OR m.tags LIKE %:keyword3% " +
                        "OR m.title LIKE %:keyword1% OR m.title LIKE %:keyword2% OR m.title LIKE %:keyword3%) " +
                        "AND m.user.isPublic = true " +
                        "AND m.user.id != :userId " + // 본인 매거진 제외
                        "AND (:lastId IS NULL OR m.id < :lastId) " +
                        "ORDER BY m.id DESC")
        java.util.List<Magazine> findRecommendedFeedCursor(
                        @org.springframework.data.repository.query.Param("keyword1") String keyword1,
                        @org.springframework.data.repository.query.Param("keyword2") String keyword2,
                        @org.springframework.data.repository.query.Param("keyword3") String keyword3,
                        @org.springframework.data.repository.query.Param("userId") Long userId,
                        @org.springframework.data.repository.query.Param("lastId") Long lastId,
                        org.springframework.data.domain.Pageable pageable);
}
