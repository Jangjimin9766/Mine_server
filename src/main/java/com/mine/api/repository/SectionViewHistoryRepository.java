package com.mine.api.repository;

import com.mine.api.domain.MagazineSection;
import com.mine.api.domain.SectionViewHistory;
import com.mine.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SectionViewHistoryRepository extends JpaRepository<SectionViewHistory, Long> {

    /**
     * 최근 열람 기록 조회 (30개, 한 달 이내)
     */
    List<SectionViewHistory> findTop30ByUserAndViewedAtAfterOrderByViewedAtDesc(
            User user, LocalDateTime after);

    /**
     * 기존 열람 기록 조회 (중복 업데이트용)
     */
    Optional<SectionViewHistory> findByUserAndSection(User user, MagazineSection section);

    /**
     * 사용자의 열람 기록 개수 조회
     */
    long countByUser(User user);

    /**
     * 가장 오래된 열람 기록 삭제 (30개 초과 시)
     */
    @Modifying
    @Query("DELETE FROM SectionViewHistory s WHERE s.user = :user AND s.id IN " +
            "(SELECT h.id FROM SectionViewHistory h WHERE h.user = :user ORDER BY h.viewedAt ASC LIMIT :count)")
    void deleteOldestByUser(@Param("user") User user, @Param("count") int count);

    /**
     * 30일 이상 지난 기록 삭제
     */
    @Modifying
    void deleteByViewedAtBefore(LocalDateTime before);
}
