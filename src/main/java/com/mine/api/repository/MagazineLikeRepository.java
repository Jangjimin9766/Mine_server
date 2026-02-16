package com.mine.api.repository;

import com.mine.api.domain.MagazineLike;
import com.mine.api.domain.Magazine;
import com.mine.api.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MagazineLikeRepository extends JpaRepository<MagazineLike, Long> {

    boolean existsByUserAndMagazine(User user, Magazine magazine);

    Optional<MagazineLike> findByUserAndMagazine(User user, Magazine magazine);

    long countByMagazine(Magazine magazine);

    @Query(value = "SELECT ml.magazine FROM MagazineLike ml " +
            "JOIN FETCH ml.magazine.user " + // N+1 방지
            "WHERE ml.user = :user", countQuery = "SELECT COUNT(ml) FROM MagazineLike ml WHERE ml.user = :user")
    Page<Magazine> findLikedMagazinesByUser(@Param("user") User user, Pageable pageable);

    // ⭐ 피드 추천용: 좋아요한 매거진 목록 (페이징 없이)
    @Query("SELECT ml.magazine FROM MagazineLike ml WHERE ml.user = :user")
    java.util.List<Magazine> findAllLikedMagazinesByUser(@Param("user") User user);
}
