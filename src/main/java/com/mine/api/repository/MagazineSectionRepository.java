package com.mine.api.repository;

import com.mine.api.domain.Magazine;
import com.mine.api.domain.MagazineSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MagazineSectionRepository extends JpaRepository<MagazineSection, Long> {

    Optional<MagazineSection> findByIdAndMagazine(Long id, Magazine magazine);
}
