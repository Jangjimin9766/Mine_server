package com.mine.api.repository;

import com.mine.api.domain.Interest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterestRepository extends JpaRepository<Interest, Long> {

    Optional<Interest> findByCode(String code);

    List<Interest> findByCategory(String category);

    boolean existsByCode(String code);
}
