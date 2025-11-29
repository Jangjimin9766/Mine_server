package com.mine.api.repository;

import com.mine.api.domain.Magazine;
import com.mine.api.domain.MagazineInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MagazineInteractionRepository extends JpaRepository<MagazineInteraction, Long> {
    List<MagazineInteraction> findByMagazineOrderByCreatedAtDesc(Magazine magazine);
}
