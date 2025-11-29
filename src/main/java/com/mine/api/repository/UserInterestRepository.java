package com.mine.api.repository;

import com.mine.api.domain.User;
import com.mine.api.domain.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {
    List<UserInterest> findByUser(User user);

    void deleteByUser(User user);
}
