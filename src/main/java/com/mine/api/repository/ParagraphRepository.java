package com.mine.api.repository;

import com.mine.api.domain.Paragraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParagraphRepository extends JpaRepository<Paragraph, Long> {

    List<Paragraph> findBySectionIdOrderByDisplayOrderAsc(Long sectionId);
}
