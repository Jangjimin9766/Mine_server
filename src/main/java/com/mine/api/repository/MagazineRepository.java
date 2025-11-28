package com.mine.api.repository;

import com.mine.api.domain.Magazine;
import com.mine.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MagazineRepository extends JpaRepository<Magazine, Long> {
    List<Magazine> findAllByUser(User user);
}
