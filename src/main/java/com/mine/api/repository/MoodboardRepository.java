package com.mine.api.repository;

import com.mine.api.domain.Moodboard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MoodboardRepository extends JpaRepository<Moodboard, Long> {
    List<Moodboard> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Moodboard> findByMagazineIdOrderByCreatedAtDesc(Long magazineId);
}
